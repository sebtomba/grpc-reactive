package reactive.server

sealed trait ServerMessage

sealed trait RequestMessage extends ServerMessage
final case class Sequence(value: Long) extends RequestMessage

sealed trait AskMessage extends ServerMessage
final case class Ask(msg: RequestMessage, sender: SenderHandle) extends AskMessage

sealed trait DiagnosticsMessage extends ServerMessage
final case class Overflow(dropped: Long) extends DiagnosticsMessage
