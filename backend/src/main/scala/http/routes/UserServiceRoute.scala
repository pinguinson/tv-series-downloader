package http.routes

import actors.DatabaseActor._
import akka.actor.{ActorSelection, ActorSystem}
import akka.dispatch.MessageDispatcher
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import http.SecurityDirectives
import models.entries.{EpisodeEntry, SubscriptionEntry}
import serializers.Protocols
import services.AuthService
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.util.{Failure, Success}
import scala.xml.XML

/**
  * Created by pinguinson on 5/11/2017.
  */
class UserServiceRoute(val authService: AuthService)(implicit system: ActorSystem, timeout: Timeout, service: ActorSelection) extends Protocols with SecurityDirectives {

  implicit val blockingDispatcher: MessageDispatcher = system.dispatchers.lookup("my-blocking-dispatcher")

  val route: Route =
    pathPrefix("user") {
      authenticate { loggedUser =>
        path("shows") {
          val serviceResponse = (service ? GetUserShows(loggedUser)).mapTo[Option[List[SubscriptionEntry]]]
          onComplete(serviceResponse) {
            case Success(Some(shows)) =>
              complete(Subscriptions(shows).toJson.prettyPrint)
            case Success(None) =>
              complete("User not found")
            case Failure(ex) =>
              complete(ex.getMessage)
          }
        } ~
          path("subscribe") {
            parameters('imdbId.as[String], 'season.as[Int], 'episode.as[Int]) { (imdbId, season, episode) =>
              val subscriptionEntry = SubscriptionEntry(loggedUser.md5, imdbId, season, episode, watching = true)
              val serviceResponse = (service ? Subscribe(loggedUser, subscriptionEntry)).mapTo[Option[SubscriptionEntry]]
              onComplete(serviceResponse) {
                case Success(Some(entry)) =>
                  complete(entry.toJson.prettyPrint)
                case Success(None) =>
                  complete("User not found")
                case Failure(ex) =>
                  complete(ex.getMessage)
              }
            }
          } ~
          path("unsubscribe") {
            parameters('imdbId.as[String]) { imdbId =>
              val serviceResponse = (service ? Unsubscribe(loggedUser, imdbId)).mapTo[Option[String]]
              onComplete(serviceResponse) {
                case Success(Some(response)) =>
                  complete(response)
                case Success(None) =>
                  complete("User not found")
                case Failure(ex) =>
                  complete(ex.getMessage)
              }
            }
          } ~
          path("feed") {
            // simply respond with user hash
            complete(loggedUser.md5.toJson)
          }
      }
    }
}
