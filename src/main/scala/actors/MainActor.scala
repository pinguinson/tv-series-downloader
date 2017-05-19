package actors

import actors.DatabaseActor._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import models.entries.EpisodeEntry

/**
  * Created by pinguinson on 5/6/2017.
  */
class MainActor extends Actor with ActorLogging {

  val dbActor: ActorRef      = context.actorOf(Props(classOf[DatabaseActor]), "dbActor")
  val tvdbActor: ActorRef    = context.actorOf(Props(classOf[TvdbActor]),     "tvdbActor")
  val torrentActor: ActorRef = context.actorOf(Props(classOf[TorrentActor]),  "torrentActor")

  def receive: Receive = {
    // user actions
    case msg: UserActionMessage =>
      dbActor forward msg
    // auth actions
    case msg: AuthActionMessage =>
      dbActor forward msg
  }
}

object MainActor {

  case class DatabaseShowEntry(episode: EpisodeEntry)

}
