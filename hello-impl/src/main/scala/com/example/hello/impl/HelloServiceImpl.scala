package com.example.hello.impl

import com.example.hello.api.HelloService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import example.myapp.helloworld.grpc.{ GreeterService, HelloRequest }

import scala.concurrent.ExecutionContext

/**
  * Implementation of the HelloService.
  */
class HelloServiceImpl(greeter: GreeterService)(implicit ex:ExecutionContext) extends HelloService {

  override def hello(id: String) = ServiceCall { _ =>
    greeter.sayHello(HelloRequest(s"Hello $id")).map(_.message).map(s => s"Received [$s] via gRPC.")
  }

}
