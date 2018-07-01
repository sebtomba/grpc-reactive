package reactive.server

import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import monix.execution.Cancelable
import monix.execution.Ack.Stop
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

      def send(responseObserver: StreamObserver[ServerReply]): StreamObserver[ClientMessage] =
        new StreamObserver[ClientMessage] {

          def onNext(value: ClientMessage): Unit = {
            if (out.onNext(Sequence(value.sequence)) == Stop)
              responseObserver.onCompleted()
            else responseObserver.onNext(ServerReply(value.sequence))
          }

          // Downstream consumer doesn't care about upstream errors
          // TODO: Log the error
          def onError(t: Throwable): Unit = ()

          // Don't shutdown the server, not calling downstream onCompleted()
          def onCompleted(): Unit = responseObserver.onCompleted()
        }
    }

    val server = ServerBuilder
      .forPort(port)
      .addService(ReactiveServiceGrpc.bindService(service, out.scheduler))
      .build
      .start

    () => server.shutdown()
  }
}
