package com.example.internal

import akka.actor.{ Actor, Props, Stash }
import akka.util.Timeout
import com.example.grpc.tools.ChannelBuilderUtils
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import io.grpc.{ Channel, ManagedChannel }

import scala.concurrent.{ ExecutionContext, Future }


object Pool {
  /**Type representing a function (implemented using a service locator) which given
    * a service name returns a new gRPC channel
    */
  type ServiceLocatedChannelFactory = (String) => Future[ManagedChannel]

  def props(channelFactory: ServiceLocatedChannelFactory)(implicit ex: ExecutionContext) = Props(new Pool(channelFactory))

  case class GetChannel(serviceName: String)

  case class PooledChannel(channel: Channel)

  case class ReturnChannel(channel: Channel)

}

// TODO: this is a PoC. Pooling will probably be provided by the akka-grpc underlying impl
class Pool(channelFactory: Pool.ServiceLocatedChannelFactory)(implicit ex: ExecutionContext) extends Actor with Stash {

  import Pool._
  import akka.pattern.pipe

  import scala.concurrent.duration._

  var channelPool: Map[String, ManagedChannel] = Map.empty
  var resolving: Set[String] = Set.empty
  implicit val timeout = Timeout(5 seconds) // needed for `?` below

  override def receive: Receive = {
    case GetChannel(serviceName) =>
      channelPool.get(serviceName) match {
        case Some(chann) => sender() ! Pool.PooledChannel(chann)
        case None if !resolving.contains(serviceName) => {
          resolving += serviceName
          channelFactory(serviceName)
            .mapTo[ManagedChannel]
            .map { ch =>
              channelPool += (serviceName -> ch)
              unstashAll()
              Pool.PooledChannel(ch)
            }
            .transform {
              x =>
                resolving -= serviceName
                x
            }
            .pipeTo(sender)
        }
        case _ => {
          stash()
        }
      }
  }
}