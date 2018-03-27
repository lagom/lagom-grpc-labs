package com.example.hello.impl

import com.example.hello.api.HelloService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import io.akka.grpc.{ Echo, EchoMessage }

import scala.concurrent.ExecutionContext

/**
  * Implementation of the HelloService.
  */
class HelloServiceImpl(echo: Echo)(implicit ex:ExecutionContext) extends HelloService {

  override def hello(id: String) = ServiceCall { _ =>
    echo.echo(EchoMessage(s"Hello $id")).map(_.payload).map(s => s"Received [$s] via gRPC.")
  }


}
