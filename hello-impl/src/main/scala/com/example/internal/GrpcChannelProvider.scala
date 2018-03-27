package com.example.internal

import akka.actor.{ ActorRef, ActorSystem }
import akka.stream.Materializer
import akka.util.Timeout
import com.example.grpc.tools.ChannelBuilderUtils
import com.example.internal.Pool.ServiceLocatedChannelFactory
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import io.grpc.Channel

import scala.concurrent.{ ExecutionContext, Future }

trait GrpcChannelProvider {
  /**
    * Obtains a Channel for the `serviceName` and invokes `block` using a client built with that channel.
    * When completing `block` the channel may be released or destroyed depending on the implementation.
    */
  def withChannel[Result](serviceName: String)(block: (Channel) => Future[Result]): Future[Result]
}

class PooledGrpcChannelProvider(serviceLocator: ServiceLocator)(implicit sys: ActorSystem, mat: Materializer, ex: ExecutionContext) extends GrpcChannelProvider {

  private val channelProvider: Pool.ServiceLocatedChannelFactory = locatorToChannelFactory(serviceLocator)

  private def locatorToChannelFactory(sl: ServiceLocator)(implicit ex: ExecutionContext): ServiceLocatedChannelFactory =
    (serviceName: String) => {
      sl.locateAll(serviceName)
        .map {
          case head :: tail => ChannelBuilderUtils.build(head.getHost, head.getPort)
          case _ => throw new RuntimeException(s"No address available for service $serviceName")
        }
    }


  import Pool._
  import akka.pattern.ask

  import scala.concurrent.duration._

  private val pool: ActorRef = sys.actorOf(Pool.props(channelProvider))
  implicit val timeout = Timeout(5 seconds) // needed for `?` below

  def withChannel[Result](serviceName: String)(block: (Channel) => Future[Result]): Future[Result] = {
    (pool ? GetChannel(serviceName)).mapTo[PooledChannel]
      .flatMap { ch =>
        block(ch.channel)
          .transform {
            x => {
              pool ! ReturnChannel(ch.channel)
              x
            }
          }
      }
  }

}
