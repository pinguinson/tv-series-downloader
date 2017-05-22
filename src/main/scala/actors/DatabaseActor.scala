package actors

import actors.DatabaseActor._
import actors.TorrentActor.LookForEpisode
import actors.TvdbActor.{FindShow, GetShowInfo, ShowEpisodes}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection}
import akka.dispatch.MessageDispatcher
import akka.pattern.pipe
import models.entries.{EpisodeEntry, ShowEntry, SubscriptionEntry, UserEntry}
import services.DatabaseService

import scala.concurrent.duration._

/**
  * Created by pinguinson on 5/6/2017.
  */
class DatabaseActor extends Actor with ActorLogging {
  implicit val blockingDispatcher: MessageDispatcher = context.system.dispatchers.lookup("my-blocking-dispatcher")

  val db = new DatabaseService("shows")

  val mainActor: ActorRef = context.parent
  val searchActor: ActorSelection = context.actorSelection("../searchActor")
  val tvdbActor: ActorSelection = context.actorSelection("../tvdbActor")
  val torrentActor: ActorSelection = context.actorSelection("../torrentActor")

  context.system.scheduler.schedule(10 seconds, 1 minute)(self ! LookForTorrents)
  context.system.scheduler.schedule(10 seconds, 1 minute)(self ! GetNewShows)
  db.createSchemaIfNotExists

  def receive: Receive = {

    // updates from other actors
    case FoundTorrent(e) =>
      db.updateEpisode(e)
    case TorrentNotFound(e) =>
      db.updateEpisode(e)

    case s: ShowEntry =>
      db.addShow(s)
      tvdbActor ! GetShowInfo(s.tvdbId)

    case ShowEpisodes(showId, episodes) =>
      db.getShowByTvdbId(showId).map {
        case Some(show) =>
          episodes.foreach { e =>
            db.addEpisode(EpisodeEntry(show.imdbId, e.airedSeason, e.airedEpisodeNumber, e.firstAired, None, None, 0))
          }
        case None =>
          log.error("Got episodes for an unknown show")
      }

    // user actions
    case Subscribe(userEntry, subscriptionEntry) =>
      db.subscribeToShow(userEntry, subscriptionEntry) pipeTo sender

    case Unsubscribe(userEntry, imdbId) =>
      db.unsubscribeFromShow(userEntry, imdbId) pipeTo sender

    case GetUserFeed(userEntry) =>
      db.getUserFeed(userEntry) pipeTo sender

    case GetUserShows(userEntry) =>
      db.getUserShows(userEntry) pipeTo sender

    // auth actions
    case SignUp(userEntry) =>
      db.signUp(userEntry) pipeTo sender



    // start regular update
    case GetNewShows =>
      for {
        newImdbIds <- db.getNewImdbIds
        imdbId <- newImdbIds
      } tvdbActor ! FindShow(imdbId)

    case LookForTorrents =>
      for {
        episodes <- db.getEpisodesWithoutTorrents
        episode <- episodes
      } torrentActor ! LookForEpisode(episode)
  }
}

object DatabaseActor {
  case class FoundTorrent(episode: EpisodeEntry)
  case class TorrentNotFound(episode: EpisodeEntry)
  case class GetShowAliases(imdbId: String)
  case object GetNewShows
  case object LookForTorrents

  // user actions
  trait UserActionMessage
  case class Subscribe(userEntry: UserEntry, subscriptionEntry: SubscriptionEntry) extends UserActionMessage
  case class Unsubscribe(userEntry: UserEntry, imdbId: String) extends UserActionMessage
  case class GetUserFeed(userEntry: UserEntry) extends UserActionMessage
  case class GetUserShows(userEntry: UserEntry) extends UserActionMessage
  case class Subscriptions(shows: List[SubscriptionEntry])

  // auth actions
  trait AuthActionMessage
  case class SignUp(userEntry: UserEntry) extends AuthActionMessage
}
