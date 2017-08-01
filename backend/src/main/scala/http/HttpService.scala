package http

import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import http.routes.{AuthServiceRoute, FeedServiceRoute, UserServiceRoute}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import services.AuthService
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration._

/**
  * Created by pinguinson on 5/9/2017.
  */

class HttpService(implicit system: ActorSystem) {
  implicit val blockingDispatcher: MessageDispatcher = system.dispatchers.lookup("my-blocking-dispatcher")

  implicit val db = Database.forConfig("shows")
  implicit val authService = new AuthService()
  implicit val timeout = Timeout(5 seconds)
  implicit val service = system.actorSelection("user/mainActor")

  val userRouter = new UserServiceRoute(authService)
  val authRouter = new AuthServiceRoute(authService)
  val feedRouter = new FeedServiceRoute()

  val route: Route = cors() {
    userRouter.route ~
      authRouter.route ~
      feedRouter.route }
}