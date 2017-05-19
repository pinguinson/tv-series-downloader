package http

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import http.routes.{AuthServiceRoute, UserServiceRoute}

/**
  * Created by pinguinson on 5/9/2017.
  */

class HttpService(implicit system: ActorSystem) {
  val userRouter = new UserServiceRoute
  val authRouter = new AuthServiceRoute

  val route: Route = userRouter.route ~ authRouter.route
}