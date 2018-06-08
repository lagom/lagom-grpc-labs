package controllers

import example.myapp.helloworld.grpc.{ GreeterService, HelloRequest }
import play.api.mvc.{ AbstractController, ControllerComponents, Results }

import scala.concurrent.{ ExecutionContext, Future }
class HelloController(client: GreeterService, controllerComponents: ControllerComponents)
                     (implicit ec: ExecutionContext)
  extends AbstractController(controllerComponents) {

  def hello() = Action.async { implicit req =>
    client.sayHello(HelloRequest(req.path)).map(rp => Results.Ok(rp.message))
//   Future.successful(Results.Ok("123"))
  }

}
