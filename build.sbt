import com.lightbend.sbt.javaagent.JavaAgent.JavaAgentKeys.javaAgents
import akka.grpc.gen.scaladsl.ScalaClientCodeGenerator

organization in ThisBuild := "com.example"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test

lazy val `lagom-grpc-labs` = (project in file("."))
  .aggregate(`hello-api`, `hello-impl`, `play-app`)

lazy val `hello-api` = (project in file("hello-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomScala,
//    JavaAgent,
    AkkaGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      macwire
    ),
  )
  .settings(
    PB.protoSources in Compile += target.value / "protobuf",
    akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client, AkkaGrpc.Server),
//    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.7" % "runtime",
    )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`hello-api`)

// !!! JavaAgent is not triggering because lagom's runAll ir not a regular sbt run but
// a custom taskKey. The current solution is to add the `javaagent` argument into
// .jvmopts and look in another direction.
// We're leaving JavaAgent and "javaAgents +=" enabled to trigger the artifact download.

lazy val `play-app` = (project in file("play-app"))
  .enablePlugins(PlayScala, LagomPlay,
    JavaAgent,
    AkkaGrpcPlugin)
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    libraryDependencies ++= Seq(
      macwire
    ),
  )
  .settings(
    PB.protoSources in Compile += target.value / "protobuf",
    akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client),
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.7" % "runtime",
  )
  .settings(lagomForkedTestSettings: _*)

lagomServicesPortRange in ThisBuild := PortRange(50000, 51000)

lagomKafkaEnabled in ThisBuild := false
lagomCassandraEnabled in ThisBuild := false

lagomUnmanagedServices in ThisBuild += ("helloworld.GreeterService" -> "http://127.0.0.1:8080")
