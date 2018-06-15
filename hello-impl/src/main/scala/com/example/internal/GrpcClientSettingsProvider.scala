package com.example.internal

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.api.ServiceLocator

import scala.concurrent.{ ExecutionContext, Future }

trait GrpcClientSettingsProvider {
  /**
    * Obtains a Channel for the `serviceName` and invokes `block` using a client built with that channel.
    * When completing `block` the channel may be released or destroyed depending on the implementation.
    */
  def withSettings[Result](serviceName: String)(block: (GrpcClientSettings) => Future[Result]): Future[Result]
}

class PooledGrpcClientSettingsProvider(serviceLocator: ServiceLocator)(implicit sys: ActorSystem, mat: Materializer, ex: ExecutionContext) extends GrpcClientSettingsProvider {

  type SettingsFactory = (String) => Future[GrpcClientSettings]

  private val settingsFactory: SettingsFactory = (name) => serviceLocator.locate(name).flatMap { maybeUri =>
    maybeUri
      .map { uri =>
        GrpcClientSettings(uri.getHost, uri.getPort)
          .withOverrideAuthority("foo.test.google.fr")
          .withTrustedCaCertificate("ca.pem")
      }
      .map(Future.successful)
      .getOrElse(Future.failed(new RuntimeException(s"Service $name not found.")))

  }

  def withSettings[Result](serviceName: String)(block: GrpcClientSettings => Future[Result]): Future[Result] = {
    settingsFactory(serviceName)
      .flatMap {
        block
      }
  }
}
