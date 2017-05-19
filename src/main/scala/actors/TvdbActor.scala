package actors

import actors.TvdbActor._
import akka.actor.{Actor, ActorLogging, ActorSelection, ActorSystem, PoisonPill}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.{Config, ConfigFactory}
import models.entries.ShowEntry
import spray.json._
import serializers.Protocols

import scala.concurrent.Future

/**
  * Created by pinguinson on 5/8/2017.
  */
class TvdbActor extends Actor with ActorLogging with Protocols {

  import context.dispatcher

  implicit val system: ActorSystem = context.system
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
  val config: Config = ConfigFactory.load()

  val credentials = Credentials(
    apikey   = config.getString("tvdbapi.secret.apikey"),
    userkey  = config.getString("tvdbapi.secret.userkey"),
    username = config.getString("tvdbapi.secret.username")
  )
  val apiEndpoint = "api.thetvdb.com"
  val apiConnectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = Http().outgoingConnectionHttps(apiEndpoint)
  var token: Option[String] = None
  val dbActor: ActorSelection = context.actorSelection("../dbActor")

  def authHeader: Option[Authorization] = token.map(t => Authorization(OAuth2BearerToken(t)))

  override def preStart() = {
    requestToken pipeTo self
  }

  def receive = {
    waitingForToken(List.empty[Message])
  }

  def waitingForToken(messages: List[Message]): Receive = {
    case AuthorisationFailed =>
      log.error(s"Failed to get TVDB API token, shutting down actor...")
      self ! PoisonPill
    case Token(t) =>
      log.info(s"Received token: ${t.substring(0, 10)}...")
      log.info(s"Switching to normal mode...")
      token = Some(t)
      context.become(normal)
      messages.foreach(self forward _)
    case msg: Message =>
      log.info(s"Received a message while waiting for a new token, saving it for later...")
      context.become(waitingForToken(msg :: messages))
  }

  def normal: Receive = {
    case GetToken =>
      log.info(s"Looks like token has expired. refreshing...")
      log.info(s"Ignoring all other messages while waiting for a new token...")
      requestToken pipeTo self
      context.become(waitingForToken(List.empty[Message]))

    case GetShowInfo(id) =>
      log.info(s"Getting information about show #$id...")
      requestShowInfo(id) pipeTo sender


    case msg@ShowEpisodes(showId, episodes) =>
      log.info(s"Received information about ${episodes.length} episodes of show #$showId")
      episodes.sortBy(_.firstAired).foreach { e =>
        val episode = f"s${e.airedSeason}%02de${e.airedEpisodeNumber}%02d"
        log.info(s"$episode '${e.episodeName}' aired on ${e.firstAired}")
      }
      dbActor ! msg

    case GetShowInfoFailed(code) =>
      log.error(s"Something went wrong while fetching show info. Response code: $code")

    case FindShow(imdbId) =>
      log.info(s"Looking for a show with imdb id: $imdbId...")
      findShowByImdbId(imdbId) pipeTo sender
  }

  def apiRequest(request: HttpRequest): Future[HttpResponse] =
    Source.single(request).via(apiConnectionFlow).runWith(Sink.head)

  // returns Token or AuthorisationFailed
  def requestToken: Future[Message] = {
    val auth = HttpEntity(ContentTypes.`application/json`, credentials.toJson.toString())
    apiRequest(RequestBuilding.Post("/login", auth)).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[Token]
      case HttpResponse(_, _, _, _) =>
        Future.successful(AuthorisationFailed)
    }
  }

  def authorisedApiGetRequest(requestBody: String): Future[Either[String, HttpResponse]] = authHeader match {
    case Some(auth) =>
      apiRequest(RequestBuilding.Get(requestBody).withHeaders(auth)).map(Right(_))
    case None =>
      Future.successful(Left("No token found"))
  }

  // returns GetShowInfoResponse, GetShowInfoFailed or AuthorisationFailed
  def requestShowInfo(showId: Int): Future[Message] = {
    authorisedApiGetRequest(s"/series/$showId/episodes?page=1").flatMap {
      case Right(HttpResponse(StatusCodes.OK, _, entity, _)) =>
        log.info(s"Received show info response, unmarshalling and converting to ShowEpisodes...")
        Unmarshal(entity).to[GetShowInfoResponse].map { res =>
          ShowEpisodes(showId, res.data.sortBy(_.firstAired))
        }
      case Right(HttpResponse(StatusCodes.Unauthorized, _, _, _)) =>
        Future.successful(GetToken)
      case Right(HttpResponse(code, _, _, _)) =>
        Future.successful(GetShowInfoFailed(code))
      case Left(_) =>
        Future.successful(AuthorisationFailed)
    }
  }

  def findShowByImdbId(imdbId: String): Future[Option[ShowEntry]] = {
    authorisedApiGetRequest(s"/search/series?imdbId=$imdbId").flatMap {
      case Right(HttpResponse(StatusCodes.OK, _, entity, _)) =>
        log.info(s"Received search result for $imdbId, unmarshalling and converting to Show entry...")
        Unmarshal(entity).to[TvdbFindShowEntry].map { s =>
          val show = ShowEntry(imdbId, s.data.head.id, s.data.head.seriesName)
          dbActor ! show
          Some(show)
        }
      case Right(HttpResponse(StatusCodes.Unauthorized, _, _, _)) =>
        self ! GetToken
        Future.successful(None)
      case Right(HttpResponse(code, _, _, _)) =>
        Future.successful(None)
      case Left(_) =>
        Future.successful(None)
    }
  }
}

object TvdbActor {

  case class Credentials(apikey: String, userkey: String, username: String)

  trait Message

  // auth
  case object GetToken extends Message
  case class Token(token: String) extends Message
  case object AuthorisationFailed extends Message

  case class GetShowInfo(id: Int) extends Message
  case class TvdbEpisode(episodeName: String, airedSeason: Int, airedEpisodeNumber: Int, firstAired: String)
  case class GetShowInfoResponse(data: List[TvdbEpisode]) extends Message
  case class GetShowInfoFailed(errorCode: StatusCode) extends Message
  case class ShowSearchEntry(id: Int, seriesName: String)
  case class TvdbFindShowEntry(data: List[ShowSearchEntry]) extends Message
  case class ShowEpisodes(showId: Int, episodes: List[TvdbEpisode]) extends Message
  case class FindShowFailed(imdbId: String, errorCode: StatusCode) extends Message
  case class FindShow(imdbId: String) extends Message

}