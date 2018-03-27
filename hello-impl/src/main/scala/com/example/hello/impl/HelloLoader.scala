package com.example.hello.impl

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.example.hello.api.HelloService
import com.example.internal.{ GrpcChannelProvider, PooledGrpcChannelProvider }
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import io.akka.grpc.{ Echo, EchoClient, EchoMessage }
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.{ ExecutionContext, Future }

class HelloLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new HelloApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HelloApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HelloService])
}

abstract class HelloApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[HelloService](wire[HelloServiceImpl])

  // TODO: this should be provided by Lagom's GrpcComponents
  private val grpcChannelFactory: GrpcChannelProvider =
    new PooledGrpcChannelProvider(serviceLocator)(actorSystem, materializer, executionContext)

  val client: Echo = wire[EchoClient]

}

// mat and ex are not implicit so `macwire` can set them
// TODO: the code below would have to be generated
class EchoClient(channelProvider: GrpcChannelProvider)(mat: Materializer, ex: ExecutionContext) extends Echo {
  private implicit val m = mat
  private implicit val e = ex

  override def echo(in: EchoMessage): Future[EchoMessage] =
    channelProvider.withChannel(Echo.name) { ch =>
      EchoClient(ch).echo(in)
    }
}