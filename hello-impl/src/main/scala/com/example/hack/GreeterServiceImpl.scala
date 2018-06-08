package com.example.hack

import akka.stream.Materializer
import example.myapp.helloworld.grpc._

import scala.concurrent.Future

class GreeterServiceImpl(materializer: Materializer) extends GreeterService {

  private implicit val mat: Materializer = materializer

  override def sayHello(in: HelloRequest): Future[HelloReply] = {
    println(s"sayHello to ${in.name}")
    Future.successful(HelloReply(s"Hello, ${in.name}"))
  }

}