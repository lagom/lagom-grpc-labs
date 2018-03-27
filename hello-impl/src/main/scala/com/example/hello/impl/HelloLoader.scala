package com.example.hello.impl

import com.example.grpc.tools.ChannelBuilderUtils
import com.example.hello.api.HelloService
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import io.akka.grpc.{ Echo, EchoClient }
import io.grpc.{ Channel, ManagedChannel, ManagedChannelBuilder }
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.Future

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


  val channel: ManagedChannel = ChannelBuilderUtils.build("127.0.0.1", 8443)

  applicationLifecycle.addStopHook { () =>
    Future.successful(channel.shutdown())
  }

  val echo: Echo = EchoClient(channel)
}
