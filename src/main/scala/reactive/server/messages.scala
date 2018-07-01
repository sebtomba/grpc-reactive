package reactive.server

sealed trait ServerMessage
final case class Sequence(value: Long) extends ServerMessage
final case class Overflow(dropped: Long) extends ServerMessage
