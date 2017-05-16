package http

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import http.routes.UserServiceRoute

/**
  * Created by pinguinson on 5/9/2017.
  */

class HttpService(implicit system: ActorSystem) {
  val userRouter = new UserServiceRoute

  val route: Route = userRouter.route
}