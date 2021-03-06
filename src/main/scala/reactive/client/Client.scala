package reactive.client

import scala.concurrent.Await
import scala.concurrent.duration._

import io.grpc.ManagedChannelBuilder
import monix.eval.Task
import monix.reactive.{Consumer, Observable}
import reactive.service._
import reactive.shared._

object Client {

  implicit val logSource: LogSource = LogSource(this.getClass)
  implicit val log: Log[Task] = reactive.shared.effects.log

  def main(args: Array[String]): Unit = {
    val channel = ManagedChannelBuilder.forAddress("localhost", DefaultPort).usePlaintext().build
    val server = ReactiveServiceGrpc.stub(channel)
    val task = Observable.intervalAtFixedRate(10.milliseconds)
      .drop(1)
      .take(1000)
      .mapParallelUnordered(10) { i =>
        if(i % 100 == 0)
          Task.fromFuture(server.ask(ServiceMessage(i)))
        else
          Task.fromFuture(server.tell(ServiceMessage(i)))
      }
      .mapTask {
        case TellReply(seq) => log.info(s"Response: $seq")
        case AskReply(seq, response) => log.info(s"Ask Response: $seq / $response")
      }
      .consumeWith(Consumer.complete)
      .doOnFinish(_ => Task.delay(channel.shutdown()))
      .runAsync(monix.execution.Scheduler.io("producer"))

    Await.ready(task, Duration.Inf)
    channel.awaitTermination(5, SECONDS)
  }
}
