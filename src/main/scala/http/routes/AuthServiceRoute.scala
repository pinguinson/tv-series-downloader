package http.routes

import actors.DatabaseActor.SignUp
import akka.http.scaladsl.server.Directives._
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.util.Timeout
import models.entries.UserEntry
import serializers.Protocols
import spray.json._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by pinguinson on 5/19/2017.
  */
class AuthServiceRoute(implicit system: ActorSystem) extends Protocols {

  private val service = system.actorSelection("user/mainActor")
  private implicit val executor: ExecutionContext = system.dispatcher
  private implicit val timeout = Timeout(5 seconds)

  val route: Route =
    pathPrefix("auth") {
      pathPrefix("signup") {
        post {
          parameters('username.as[String], 'passwordHash.as[String]) { (username, passwordHash) =>
            val userEntry = UserEntry(username, passwordHash)
            val serviceResponse = (service ? SignUp(userEntry)).mapTo[Option[UserEntry]]
            onComplete(serviceResponse) {
              case Success(Some(user)) =>
                complete(s"Registered user ${user.username}")
              case Success(None) =>
                complete(s"User ${userEntry.username} is already registered")
              case Failure(ex) =>
                complete(ex.getMessage)
            }
          }
        }
      }
    }

}
