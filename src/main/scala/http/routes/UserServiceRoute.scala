package http.routes

import actors.DatabaseActor.{GetUserFeed, Subscribe, Unsubscribe}
import akka.http.scaladsl.server.Directives._
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.util.Timeout
import models.entries.{EpisodeEntry, SubscriptionEntry}
import models.CCSerialization._
import spray.json._

import scala.xml.XML
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by pinguinson on 5/11/2017.
  */
class UserServiceRoute(implicit system: ActorSystem) {
  private val service = system.actorSelection("user/mainActor")
  private implicit val executor: ExecutionContext = system.dispatcher
  private implicit val timeout = Timeout(5 seconds)

  val route: Route =
    pathPrefix("user") {
      pathPrefix("subscribe") {
        post {
          parameters('userHash.as[String], 'imdbId.as[String], 'season.as[Int], 'episode.as[Int]) { (userHash, imdbId, season, episode) =>
            val subscriptionEntry = SubscriptionEntry(userHash, imdbId, season, episode, watching = true)
            val serviceResponse = (service ? Subscribe(subscriptionEntry)).mapTo[Option[SubscriptionEntry]]
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
          parameters('userHash.as[String], 'imdbId.as[String]) { (userHash, imdbId) =>
            val serviceResponse = (service ? Unsubscribe(userHash, imdbId)).mapTo[Option[String]]
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
            parameters('userHash.as[String]) { userHash =>
              val serviceResponse = (service ? GetUserFeed(userHash)).mapTo[Option[List[EpisodeEntry]]]
              onComplete(serviceResponse) {
                case Success(Some(episodes)) =>
                  val rss =
                    <rss version="2.0">

                    <channel>
                      <title>Your RSS feed, {userHash}</title>
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
