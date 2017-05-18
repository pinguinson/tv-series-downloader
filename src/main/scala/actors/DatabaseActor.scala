package actors

import actors.DatabaseActor._
import actors.TorrentActor.LookForEpisode
import actors.TvdbActor.{FindShow, GetShowInfo, ShowEpisodes}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection}
import akka.dispatch.MessageDispatcher
import akka.pattern.pipe
import models.entries.{EpisodeEntry, ShowEntry, SubscriptionEntry}
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
    case Subscribe(subscription) =>
      db.subscribeToShow(subscription) pipeTo sender

    case Unsubscribe(userHash, imdbId) =>
      db.unsubscribeFromShow(userHash, imdbId) pipeTo sender

    case GetUserFeed(userHash) =>
      db.getUserFeed(userHash) pipeTo sender


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
  case class Subscribe(subscriptionEntry: SubscriptionEntry) extends UserActionMessage
  case class Unsubscribe(userHash: String, imdbId: String) extends UserActionMessage
  case class GetUserFeed(userHash: String) extends UserActionMessage
  case class GetUserShows(userHash: String) extends UserActionMessage
}
