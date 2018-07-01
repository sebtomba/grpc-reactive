package reactive.shared

import monix.eval.Task

package object effects {
  def log: Log[Task] = new Log[Task] {
    def debug(msg: String)(implicit ev: LogSource): Task[Unit] =
      Task.delay(Logger(ev.clazz).debug(msg))
    def info(msg: String)(implicit ev: LogSource): Task[Unit] =
      Task.delay(Logger(ev.clazz).info(msg))
    def warn(msg: String)(implicit ev: LogSource): Task[Unit] =
      Task.delay(Logger(ev.clazz).warn(msg))
    def error(msg: String)(implicit ev: LogSource): Task[Unit] =
      Task.delay(Logger(ev.clazz).error(msg))
  }

}
