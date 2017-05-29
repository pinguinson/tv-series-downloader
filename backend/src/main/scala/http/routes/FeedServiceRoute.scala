package http.routes

import akka.actor.{ActorSelection, ActorSystem}
import akka.dispatch.MessageDispatcher
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import http.SecurityDirectives
import models.entries.UserEntry
import serializers.Protocols
import services.AuthService
import util.MD5.hash

import scala.util.{Failure, Success}

/**
  * Created by pinguinson on 5/19/2017.
  */
class FeedServiceRoute(implicit system: ActorSystem, timeout: Timeout, service: ActorSelection) extends Protocols {

  implicit val blockingDispatcher: MessageDispatcher = system.dispatchers.lookup("my-blocking-dispatcher")

  val route: Route =
    path("feed" / IntNumber) {
      path("signUp") {
        parameters('username.as[String], 'password.as[String]) { (username, password) =>
          val userEntry = UserEntry(username, hash(password))
          onComplete(signUp(userEntry)) {
            case Success(Some(tokenEntry)) =>
              complete(s"Registered user $tokenEntry")
            case Success(None) =>
              complete(s"User with name $username already exists")
            case Failure(ex) =>
              complete(ex.getMessage)
          }
        }
      } ~
        path("signIn") {
          parameters('username.as[String], 'password.as[String]) { (username, password) =>
            val userEntry = UserEntry(username, hash(password))
            onComplete(signIn(userEntry)) {
              case Success(Some(tokenEntry)) =>
                giveToken(tokenEntry)
              case Success(None) =>
                complete(s"User with name $username and password $password not found")
              case Failure(ex) =>
                complete(ex.getMessage)
            }
          }
        } ~
        path("signOut") {
          deleteToken()
        }
    }
}
