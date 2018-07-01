package reactive.server

import scala.concurrent.Future

import io.grpc.ServerBuilder
import monix.execution.Cancelable
import monix.reactive.Observable
import monix.reactive.observers.{BufferedSubscriber, Subscriber}
import monix.reactive.OverflowStrategy.DropNewAndSignal
import reactive.service._

class ServerObservable(port: Int, bufferSize: Int) extends Observable[ServerMessage] {

  private def overflowMessage(count: Long): Option[ServerMessage] =
    Some(Overflow(count))

  def unsafeSubscribeFn(subscriber: Subscriber[ServerMessage]): Cancelable = {
    val out = BufferedSubscriber[ServerMessage](subscriber, DropNewAndSignal(bufferSize, overflowMessage))

    val service = new ReactiveServiceGrpc.ReactiveService {
      def send(request: ClientMessage): Future[ServerReply] =
        out.onNext(Sequence(request.sequence)).map(_ => ServerReply(request.sequence))(out.scheduler)
    }

    val server = ServerBuilder
      .forPort(port)
      .addService(ReactiveServiceGrpc.bindService(service, out.scheduler))
      .build
      .start

    () => server.shutdown()
  }
}
