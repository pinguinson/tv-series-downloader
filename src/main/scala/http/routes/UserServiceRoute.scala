package http.routes

import actors.DatabaseActor._
import akka.actor.{ActorSelection, ActorSystem}
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

import scala.util.{Failure, Success}
import scala.xml.XML

/**
  * Created by pinguinson on 5/11/2017.
  */
class UserServiceRoute(val authService: AuthService)(implicit system: ActorSystem, timeout: Timeout, service: ActorSelection) extends Protocols with SecurityDirectives {

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
            val serviceResponse = (service ? GetUserFeed(loggedUser)).mapTo[Option[List[EpisodeEntry]]]
            onComplete(serviceResponse) {
              case Success(Some(episodes)) =>
                val rss =
                  <rss version="2.0">
                    <channel>
                      <title>Your RSS feed, {loggedUser.username}</title>
                      <link>https://github.com/pinguinson/tv-series-downloader</link>
                      <description>RSS feed with torrents of new episodes of your favourite shows</description>
                      {episodes.sortBy(_.airDate).reverse.map(_.toXml)}
                    </channel>
                  </rss>
                val writer = new java.io.StringWriter
                XML.write(writer, rss, "utf-8", xmlDecl = true, doctype = null)
                complete {
                  HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/rss+xml`, HttpCharsets.`UTF-8`), writer.toString))
                }
              case Success(None) =>
                complete("User not found")
              case Failure(ex) =>
                complete(ex.getMessage)
            }
          }
      }
    }
}
