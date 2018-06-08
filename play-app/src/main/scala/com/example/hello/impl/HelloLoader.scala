package com.example.hello.impl

import akka.grpc.GrpcClientSettings
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.api.{ LagomConfigComponent, ServiceAcl, ServiceInfo }
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.softwaremill.macwire._
import controllers.HelloController
import example.myapp.helloworld.grpc.{ GreeterService, HelloReply, HelloRequest }
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.{ ApplicationLoader, BuiltInComponentsFromContext }
import play.filters.HttpFiltersComponents
import router.Routes

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }

class HelloLoader extends ApplicationLoader {
  def load(context: Context) = {
    (new HelloApplication(context) with LagomDevModeComponents).application
  }
}

abstract class HelloApplication(context: Context)
  extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with HttpFiltersComponents
    with LagomConfigComponent
    with LagomServiceClientComponents {

  lazy val serviceInfo: ServiceInfo = ServiceInfo(
    "web-gateway",
    Map(
      "web-gateway" -> immutable.Seq(ServiceAcl.forPathRegex("(?!/api/).*"))
    )
  )
  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher

  override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }

  val client: GreeterService = wire[GreeterServiceClient]

  val hello: HelloController = wire[HelloController]

}

class GreeterServiceClient(mat: Materializer, ex: ExecutionContext) extends GreeterService {
  private implicit val materializer = mat
  private implicit val executionContext = ex

  val settings = GrpcClientSettings("localhost", 8080)
    .withOverrideAuthority("foo.test.google.fr")
    .withCertificate("ca.pem")

  override def sayHello(in: HelloRequest): Future[HelloReply] =
    example.myapp.helloworld.grpc.GreeterServiceClient(settings).sayHello(in)
}

