package reactive.server

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

import monix.eval.Task
import monix.execution._
import monix.reactive._
import reactive.service._
import reactive.shared._

object Server {

  implicit val logSource: LogSource = LogSource(this.getClass)
  implicit val log: Log[Task] = reactive.shared.effects.log

  def main(args: Array[String]): Unit = {
    val parallelism = Runtime.getRuntime.availableProcessors()
    val task = receive(parallelism)(dispatch)
      .runAsync(Scheduler.computation(name = "consumer", executionModel = ExecutionModel.AlwaysAsyncExecution))

    Await.ready(task, Duration.Inf)
  }

  def server(port: Int): Observable[ServerMessage] =
    new ServerObservable(port, 100, 10)

  def receive(parallelism: Int)(dispatch: ServerMessage => Task[Unit]): Task[Unit] =
    server(DefaultPort)
      .mapParallelUnordered(parallelism)(dispatch)
      .consumeWith(Consumer.complete)

  def dispatch(message: ServerMessage)(implicit log: Log[Task]): Task[Unit] =
    message match {
      case Sequence(seq) =>
        log.info(s"Consuming $seq").delayResult(((Random.nextInt(10) + 1) * 100).milliseconds)

      case Overflow(count) =>
        log.warn(s"Dropped $count messages")

      case Ask(Sequence(seq), sender) =>
        log.info(s"Consuming Ask $seq")
          .delayResult(((Random.nextInt(3) + 1) * 100).milliseconds)
          .foreachL(_ => sender.asInstanceOf[ReplyPromise].reply("ok"))
    }
}
