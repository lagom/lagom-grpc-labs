# Lagom to gRPC lab 

This project uses a HelloService from Lagom's `g8` to send a request to the server provided by https://github.com/ignasi35/akka-grpc-labs.

Clone both repos. Start the server in https://github.com/ignasi35/akka-grpc-labs and the use `sbt runAll` to start the lagom service here.

Finally test using `curl http://localhost:9000/api/hello/arthur`