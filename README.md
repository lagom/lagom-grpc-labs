# Lagom to gRPC lab 

This project uses Lagom's DevMode to start a Lagom Service and a Play Application (from now on referred to as "the proxy services").

The proxy services are reachable at:

```
$ curl http://localhost:9000/api/hello/arthur  ## the lagom service
$ curl http://localhost:9000/play/hello        ## the play app
```

These two proxy services offer a single HTTP/1.1 endpoint each that proxies the request to "localhost:8080" using an akka-grpc client. 

This project runs an embedded akka-grpc server bound to port 8080. This is embedded inside the Lagom service so that when using `runAll`, the lagom service will start and the lagom service will also start the akka-grpc server. Summing up: there are 2 processes with a total of three services:

 * lagom service process: lagom service endpoint + embedded akka-grpc server on port 8080
 * play app process: play app endpoint

Again, running an embedded akka-grpc is a hack until play and lagom can serve grpc.

The `.proto` used between the two is a simplified version of the example in the [Akka-gRPC docs](https://developer.lightbend.com/docs/akka-grpc/current/)

## Preparing

This repo depends on a local build of [akka-grpc#7d6b719](https://github.com/akka/akka-grpc/tree/7d6b719d42fea56379bac52155ab407afa9434ad), so clone that and build it locally.

**ATTENTION**: the injection of a javaAgent is reguired but this remains an unsolved issue. Edit the `.jvmopts` file to set the absolute path to the ALPN agent in your local ivy cache.

## Running

Run `sbt runAll`.

Finally test using `curl http://localhost:9000/api/hello/arthur` or `curl http://localhost:9000/play/hello`.

## Purpose

The goal of this lab is to find investigate Dev eXperience alternatives so that building a gRPC client that runs within a Lagom service (or a Play app) is as simple as:

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


 * `src/main/resources/certs` contains some baked certs, PEMs and keys to use during testing with SSL enabled. Note that the clients only use `ca.pem` while the hhacked embedded server uses the `key.*`
 * `src/main/scala`
    * `com.example.internal` is code that could eventually be part of Lagom
    * `com.example.hello.impl` is userland code
        * `HelloLoader.scala` contains the `GreetinsServiceClient` that would be generated via Lagom's GrpcGenerators using `akka-grpc-sbt-plugin` infra. Maybe Lagom extends the `akka-grpc-sbt-plugin` to simplify.
        * `HelloLoader.scala` contains a regular Lagom `HelloLoader` that mixes in the GrpcComponents and wires an `EchoClient`

The Play code is very similar to the Lagom service code but simpler. We developed the Play App using Maven Layout to ease comparing both codebases.
        
        
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
