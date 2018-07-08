package reactive.server

import scala.concurrent.{Future, TimeoutException}
import scala.concurrent.duration._

import io.grpc.ServerBuilder
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import monix.reactive.observers.{BufferedSubscriber, Subscriber}
import monix.reactive.OverflowStrategy.{BackPressure, DropNewAndSignal}
import monix.reactive.subjects.ConcurrentSubject
import reactive.service._

class ServerObservable(
  port: Int,
  tellBufferSize: Int = 1024,
  askBufferSize: Int = 128,
  askTimeout: FiniteDuration = 2.second
) extends Observable[ServerMessage] {

  private def overflowMessage(count: Long): Option[ServerMessage] =
    Some(Overflow(count))

  def unsafeSubscribeFn(subscriber: Subscriber[ServerMessage]): Cancelable = {
    implicit val scheduler: Scheduler = subscriber.scheduler

    val subjectTell = ConcurrentSubject.publishToOne(DropNewAndSignal(tellBufferSize, overflowMessage))
    val subjectAsk = ConcurrentSubject.publishToOne(DropNewAndSignal(askBufferSize, overflowMessage))
    val merged = Observable.merge(subjectTell, subjectAsk)(BackPressure(10))

    val service = new ReactiveServiceGrpc.ReactiveService {

      def tell(request: ServiceMessage): Future[TellReply] =
        subjectTell.onNext(Sequence(request.sequence)).map(_ => TellReply(request.sequence))

      def ask(request: ServiceMessage): Future[AskReply] = {
        val p = ReplyPromise(askTimeout)
        val result = for {
          _ <- subjectAsk.onNext(Ask(Sequence(request.sequence), p))
          reply <- p.future
        } yield AskReply(request.sequence, reply)

        result.recover {
          case _: TimeoutException => AskReply(request.sequence, "timeout")
        }
      }
    }

    val mergeSubscription = merged.subscribe(subscriber)

    val server = ServerBuilder
      .forPort(port)
      .addService(ReactiveServiceGrpc.bindService(service, scheduler))
      .build
      .start

    () => {
      server.shutdown().awaitTermination()
      mergeSubscription.cancel()
    }
  }
}
