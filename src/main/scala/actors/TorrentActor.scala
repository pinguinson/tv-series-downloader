package actors

import actors.DatabaseActor.{FoundTorrent, TorrentNotFound}
import actors.TorrentActor._
import akka.actor.{Actor, ActorLogging, ActorSelection, ActorSystem, PoisonPill}
import akka.dispatch.MessageDispatcher
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import akka.stream.QueueOfferResult.Enqueued
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy, ThrottleMode}
import akka.util.ByteString
import serializers.Protocols
import models.entries.EpisodeEntry
import spray.json._

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
/**
  * Created by pinguinson on 5/10/2017.
  */
class TorrentActor extends Actor with ActorLogging with Protocols {

  implicit val blockingDispatcher: MessageDispatcher = context.system.dispatchers.lookup("my-blocking-dispatcher")
  implicit val system: ActorSystem = context.system
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))

  val dbActor: ActorSelection = context.actorSelection("../dbActor")

  val apiHost = "torrentapi.org"
  val apiEndpoint = "/pubapi_v2.php"
  val requiredQuality = "720p"
  var token: Option[String] = None

  // rarbg api has a 1req/2s limit, using throttling
  val pool = Http().cachedHostConnectionPool[Promise[HttpResponse]](host = apiHost, port = 80).throttle(1, 2 second, 1, ThrottleMode.Shaping)
  val queue = Source.queue[(HttpRequest, Promise[HttpResponse])](1000, OverflowStrategy.dropHead)
    .via(pool)
    .toMat(Sink.foreach {
      case ((Success(resp), p)) => p.success(resp)
      case ((Failure(e), p)) => p.failure(e)
    })(Keep.left)
    .run

  override def preStart() = {
    requestToken pipeTo self
  }

  def receive = {
    waitingForToken(List.empty)
  }

  def waitingForToken(messages: List[Any]): Receive = {
    case AuthorisationFailed =>
      log.error(s"Failed to get rarbg API token, shutting down actor...")
      self ! PoisonPill
    case Token(t) =>
      log.info(s"Received token: $t")
      log.info(s"Switching to normal mode...")
      token = Some(t)
      context.become(normal)
      messages.foreach(self forward)
    case msg =>
      log.info(s"Received a message while waiting for a new token, saving it for later...")
      context.become(waitingForToken(msg :: messages))
  }

  def normal: Receive = {
    case LookForEpisode(episode) =>
      log.info(s"TorrentActor: looking for torrent for $episode")
      searchForEpisode(episode).map {
        case RarbgResponse(head :: _) =>
          log.info(s"Found torrent for $episode")
          dbActor ! FoundTorrent(episode.copy(
            filename = Some(head.title),
            magnet   = Some(head.download),
            searches = episode.searches + 1)
          )
        case _ =>
          dbActor ! TorrentNotFound(episode.copy(searches = episode.searches + 1))
          log.info(s"Failed to find torrent for $episode")
      }
  }

  def sendRequest(params: Map[String, String]): Future[HttpResponse] = {
    val promise = Promise[HttpResponse]
    val request = HttpRequest(uri = Uri(apiEndpoint).withQuery(Query(params))) -> promise

    queue.offer(request).flatMap {
      case Enqueued => promise.future
      case _ => Future.failed(new RuntimeException())
    }
  }

  def buildSearchString(episodeEntry: EpisodeEntry): String = {
    f"s${episodeEntry.season}%02de${episodeEntry.episode}%02d"
  }

  def requestToken: Future[Message] = {
    val params = Map(
      "get_token" -> "get_token"
    )
    sendRequest(params).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[Token]
      case HttpResponse(_, _, _, _) =>
        Future.successful(AuthorisationFailed)
    }
  }

  def searchForEpisode(episodeEntry: EpisodeEntry): Future[RarbgResponse] = {
    val params = Map(
      "mode" -> "search",
      "search_imdb" -> episodeEntry.imdbId,
      "search_string" -> s"${buildSearchString(episodeEntry)} $requiredQuality",
      "token" -> token.getOrElse("notoken"),
      "format" -> "json_extended",
      "sort" -> "seeders"
    )
    sendRequest(params).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
          if (body.utf8String contains "error_code")
            RarbgResponse(List.empty)
          else
            body.utf8String.parseJson.convertTo[RarbgResponse]
        }
      case HttpResponse(_, _, _, _) =>
        //TODO: handle other responses
        Future.successful(RarbgResponse(List.empty))
    }
  }
}

object TorrentActor {
  trait Message
  case object AuthorisationFailed extends Message
  case class Token(token: String) extends Message
  case class LookForEpisode(episodeEntry: EpisodeEntry) extends Message

  case class EpisodeInfo(imdb: String, tvdb: String, seasonnum: String, epnum: String)
  case class EpisodeTorrent(title: String, download: String, seeders: Int, leechers: Int)
  case class RarbgResponse(torrent_results: List[EpisodeTorrent])
}