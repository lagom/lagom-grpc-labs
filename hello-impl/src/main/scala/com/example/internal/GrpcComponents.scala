package com.example.internal

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.api.ServiceLocator

import scala.concurrent.ExecutionContext

/**
  *
  */
trait GrpcComponents {
  def serviceLocator: ServiceLocator
  def actorSystem: ActorSystem
  def materializer: Materializer
  def executionContext: ExecutionContext

  val grpcChannelFactory: GrpcClientSettingsProvider =
    new PooledGrpcClientSettingsProvider(serviceLocator)(actorSystem, materializer, executionContext)
}
