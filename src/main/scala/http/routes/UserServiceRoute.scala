package http.routes

import actors.DatabaseActor.{GetUserFeed, Subscribe, Unsubscribe}
import akka.http.scaladsl.server.Directives._
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.util.Timeout
import models.entries.{EpisodeEntry, SubscriptionEntry, UserEntry}
import serializers.Protocols
import spray.json._

import scala.xml.XML
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by pinguinson on 5/11/2017.
  */
class UserServiceRoute(implicit system: ActorSystem) extends Protocols {
  private val service = system.actorSelection("user/mainActor")
  private implicit val executor: ExecutionContext = system.dispatcher
  private implicit val timeout = Timeout(5 seconds)

  val route: Route =
    pathPrefix("user") {
      pathPrefix("subscribe") {
        post {
          parameters('username.as[String], 'passwordHash.as[String], 'imdbId.as[String], 'season.as[Int], 'episode.as[Int]) { (username, passwordHash, imdbId, season, episode) =>
            val userEntry = UserEntry(username, passwordHash)
            val subscriptionEntry = SubscriptionEntry(userEntry.md5, imdbId, season, episode, watching = true)
            val serviceResponse = (service ? Subscribe(userEntry, subscriptionEntry)).mapTo[Option[SubscriptionEntry]]
            onComplete(serviceResponse) {
              case Success(Some(entry)) =>
                complete(entry.toJson.prettyPrint)
              case Success(None) =>
                complete("User not found")
              case Failure(ex) =>
                complete(ex.getMessage)
            }
          }
        }
      } ~
      pathPrefix("unsubscribe") {
        post {
          parameters('username.as[String], 'passwordHash.as[String], 'imdbId.as[String]) { (username, passwordHash, imdbId) =>
            val userEntry = UserEntry(username, passwordHash)
            val serviceResponse = (service ? Unsubscribe(userEntry, imdbId)).mapTo[Option[String]]
            onComplete(serviceResponse) {
              case Success(Some(response)) =>
                complete(response)
              case Success(None) =>
                complete("User not found")
              case Failure(ex) =>
                complete(ex.getMessage)
            }
          }
        }
      } ~
      pathPrefix("feed") {
        pathEndOrSingleSlash {
          get {
            parameters('username.as[String], 'passwordHash.as[String]) { (username, passwordHash) =>
              val userEntry = UserEntry(username, passwordHash)
              val serviceResponse = (service ? GetUserFeed(userEntry)).mapTo[Option[List[EpisodeEntry]]]
              onComplete(serviceResponse) {
                case Success(Some(episodes)) =>
                  val rss =
                    <rss version="2.0">

                    <channel>
                      <title>Your RSS feed, {userEntry.username}</title>
                      <link>https://github.com/pinguinson/tv-series-downloader</link>
                      <description>RSS feed with torrents of new episodes of your favourite shows</description>
                      { episodes.sortBy(_.airDate).reverse.map(_.toXml) }
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
    }
}
