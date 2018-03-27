package com.example.hello.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}

trait HelloService extends Service {

  /**
    * Example: curl http://localhost:9000/api/hello/Alice
    */
  def hello(id: String): ServiceCall[NotUsed, String]

  override final def descriptor = {
    import Service._
    named("hello")
      .withCalls(
        pathCall("/api/hello/:id", hello _)
      )
      .withAutoAcl(true)
  }
}
