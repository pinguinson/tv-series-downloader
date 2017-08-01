package http.routes

import actors.DatabaseActor.GetUserFeedByHash
import akka.actor.{ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.dispatch.MessageDispatcher
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import http.SecurityDirectives
import models.entries.{EpisodeEntry, UserEntry}
import serializers.Protocols
import services.AuthService
import util.MD5.hash

import scala.util.{Failure, Success}
import scala.xml.XML

/**
  * Created by pinguinson on 5/19/2017.
  */
class FeedServiceRoute(implicit system: ActorSystem, timeout: Timeout, service: ActorSelection) extends Protocols {

  implicit val blockingDispatcher: MessageDispatcher = system.dispatchers.lookup("my-blocking-dispatcher")

  val route: Route =
    pathPrefix("feed" / RemainingPath) { p =>
      val hash = p.toString
      val serviceResponse = (service ? GetUserFeedByHash(hash)).mapTo[List[EpisodeEntry]]
      onComplete(serviceResponse) {
        case Success(episodes) =>
          val rss =
            <rss version="2.0">
              <channel>
                <title>Your RSS feed</title>
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
        case Failure(ex) =>
          complete(ex.getMessage)
      }
    }
}
