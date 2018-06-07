# Lagom to gRPC lab 

This project uses a Lagom Service to send a request to the server provided by [https://github.com/ignasi35/akka-grpc-labs](https://github.com/ignasi35/akka-grpc-labs). The `.proto` used between the two is a simplified version of the example in the [Akka-gRPC docs](https://developer.lightbend.com/docs/akka-grpc/current/)

## Preparing

Clone [that](https://github.com/ignasi35/akka-grpc-labs) repo and this repo.

Both repos depend on a local build of [akka-grpc#7d6b719](https://github.com/akka/akka-grpc/tree/7d6b719d42fea56379bac52155ab407afa9434ad), so clone that and build it locally.

## Running

Start the server in https://github.com/ignasi35/akka-grpc-labs using and then use `sbt runAll` here to start the lagom service (which acts as proxy).

Finally test using `curl http://localhost:9000/api/hello/arthur`.

## Purpose

The goal of this lab is to find investigate Dev eXperience alternatives so that building a gRPC client that runs within a Lagom service is as simple as:

(the following is just an alternative, not a final design)

```scala
abstract class HelloApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents
    with GrpcComponents { // Add this (1) 

  val client: GreetingsService = wire[GreetingsServiceClient]  // Add this to use a client to a remote GrpcEchoServer (2)

  // regular Lagom stuff creating the Service. (3) 
  override lazy val lagomServer = serverFor[HelloService](wire[HelloServiceImpl])
}

```

1. Use a `Components`
2. Wire the gRPC interface to a client
3. Inject thhe client into regular code.

## Code overview

All the interesting code is on `hello-impl`

 * `src/main/resources/certs` contains some baked certs, PEMs and keys to use during testing with SSL enabled.
 * `src/main/scala`
    * `com.example.internal` is code that could eventually be part of Lagom
    * `com.example.hello.impl` is userland code
        * `HelloLoader.scala` contains the `GreetinsServiceClient` that would be generated via Lagom's GrpcGenerators using `akka-grpc-sbt-plugin` infra. Maybe Lagom extends the `akka-grpc-sbt-plugin` to simplify.
        * `HelloLoader.scala` contains a regular Lagom `HelloLoader` that mixes in the GrpcComponents and wires an `EchoClient`
        
        
## TODO (obsolete, but please don't delete)

1. Support Streamed calls.

2. Improve the UX in `build.sbt`:

```scala
lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomScala, JavaAgent, AkkaGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      macwire,
      scalaTest
    ),
  )
  .settings(
    PB.protoSources in Compile += target.value / "protobuf",
    (akkaGrpcCodeGenerators in Compile) := Seq(
      GeneratorAndSettings(ScalaClientCodeGenerator, (akkaGrpcCodeGeneratorSettings in Compile).value)),
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.6" % "runtime",
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`hello-api`)
```

3. Manage SSL on dev mode. We'll probably need to allow the setup of CA's on the build so that users can inject their organizationCA when testing with gRPC services implemented by other teams

4. ...