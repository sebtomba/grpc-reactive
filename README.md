# gRPC reactive stream processing

Proof of concept. GRPC server as a Monix reactive `Observable` with a `BufferedSubscriber`, dropping new messages on overflow.
