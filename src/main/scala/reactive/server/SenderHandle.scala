package reactive.server

import java.util.concurrent.TimeoutException

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.FiniteDuration

import monix.execution.Scheduler

trait SenderHandle

final class ReplyPromise(timeout: FiniteDuration)(implicit scheduler: Scheduler) extends SenderHandle {
  private val promise = Promise[String]

  lazy val future: Future[String] = {
    val err = new TimeoutException
    val task = scheduler.scheduleOnce(timeout.length, timeout.unit, () => promise.tryFailure(err))
    promise.future.andThen { case _ => task.cancel() }
  }

  def reply(msg: String): Boolean = promise.trySuccess(msg)
}

object ReplyPromise {
  def apply(timeout: FiniteDuration)(implicit scheduler: Scheduler): ReplyPromise = new ReplyPromise(timeout)
}
