package reactive.shared

trait LogSource {
  val clazz: Class[_]
}

object LogSource {
  def apply(c: Class[_]) = new LogSource {
    val clazz: Class[_] = c
  }
}

trait Log[F[_]] {
  def debug(msg: String)(implicit ev: LogSource): F[Unit]
  def info(msg: String)(implicit ev: LogSource): F[Unit]
  def warn(msg: String)(implicit ev: LogSource): F[Unit]
  def error(msg: String)(implicit ev: LogSource): F[Unit]
}
