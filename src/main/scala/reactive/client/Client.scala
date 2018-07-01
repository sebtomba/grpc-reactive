package reactive.client

import scala.concurrent.Await
import scala.concurrent.duration._

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import io.grpc.stub.StreamObserver
import monix.eval.Task
import monix.execution.Cancelable
import monix.reactive.{Consumer, Observable}
import monix.reactive.observers.Subscriber
import reactive.service._
import reactive.shared._

object Client {

  implicit val logSource: LogSource = LogSource(this.getClass)
  implicit val log: Log[Task] = reactive.shared.effects.log


  def main(args: Array[String]): Unit = {
    val channel = ManagedChannelBuilder.forAddress("localhost", DefaultPort).usePlaintext().build
    val task = new ServerReplyObservable(channel)
      .mapTask(r => log.info(s"Response: ${r.sequence}"))
      .consumeWith(Consumer.complete)
      .doOnFinish(_ => Task.delay(channel.shutdown()))
      .runAsync(monix.execution.Scheduler.io("producer"))

    Await.ready(task, Duration.Inf)
    channel.awaitTermination(5, SECONDS)
  }
}

class ServerReplyObservable(channel: ManagedChannel) extends Observable[ServerReply] {

  def unsafeSubscribeFn(subscriber: Subscriber[ServerReply]): Cancelable = {
    val server: ReactiveServiceGrpc.ReactiveServiceStub = ReactiveServiceGrpc.stub(channel)

    val observer = new StreamObserver[ServerReply] {
      def onNext(value: ServerReply): Unit = subscriber.onNext(value)
      def onError(t: Throwable): Unit = subscriber.onError(t)
      def onCompleted(): Unit = subscriber.onComplete()
    }

    val out = server.send(observer)

    Observable.intervalAtFixedRate(10.milliseconds)
      .drop(1)
      .take(1000)
      .consumeWith(Consumer.foreach(i => out.onNext(ClientMessage(i))))
      .doOnFinish {
        case Some(t) => Task.delay(out.onError(t))
        case _ => Task.delay(out.onCompleted())
      }
      .runAsync(monix.execution.Scheduler.io())
  }
}
