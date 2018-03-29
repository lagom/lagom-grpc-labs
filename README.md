# Lagom to gRPC lab 

This project uses a HelloService from Lagom's `g8` to send a request to the server provided by https://github.com/ignasi35/akka-grpc-labs.

Clone both repos. Start the server in https://github.com/ignasi35/akka-grpc-labs and the use `sbt runAll` to start the lagom service here.

Finally test using `curl http://localhost:9000/api/hello/arthur`

## Purpose

The final goal of this lab is to find potential ways so that building a gRPC client that runs within a Lagom service is as simple as:

```scala
abstract class HelloApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents
    with GrpcComponents { // Add this

  val client: Echo = wire[EchoClient]  // Add this to use a client to a remote GrpcEchoServer

  // regular Lagom stuff creating the Service.
  override lazy val lagomServer = serverFor[HelloService](wire[HelloServiceImpl])
}

```


## Code overview

All the interesting code is on `hello-impl`

 * `src/main/resources/certs` contains some baked certs, PEMs and keys to use during testing with SSL enabled.
 * `src/main/scala`
    * `com.example.grpc.tools` contains some code to use during tests with SSL enabled
    * `com.example.internal` is code that eventually will be part of Lagom
    * `com.example.hello.impl` is userland code
        * `HelloLoader.scala` contains the `EchoClient` that would be generated via Lagom's GrpcGenerators using `akka-grpc-sbt-plugin` infra. Maybe Lagom extends the `akka-grpc-sbt-plugin` to simplify.
        * `HelloLoader.scala` contains a regular Lagom `HelloLoader` that mixes in the GrpcComponents and wires an `EchoClient`
        
## TODO

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