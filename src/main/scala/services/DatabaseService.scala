package services

import models.entries.{EpisodeEntry, ShowEntry, SubscriptionEntry}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.util.Logging

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
/**
  * Created by pinguinson on 5/5/2017.
  */
class DatabaseService(dbConfigName: String)(implicit executionContext: ExecutionContext) extends Logging {
  private implicit val db: PostgresProfile.backend.Database = Database.forConfig(dbConfigName)
  private val episodeService = new EpisodeService
  private val showService = new ShowService
  private val subscriptionService = new SubscriptionService
  private val userService = new UserService

  import showService.shows
  import subscriptionService.subscriptions

  def getNewImdbIds: Future[Seq[String]] = {
    val query = for {
      (sub, show) <- subscriptions joinLeft shows on (_.imdbId === _.imdbId)
      if show.isEmpty
    } yield sub.imdbId

    db.run(query.result)
  }

  private def processUserRequest[T](userHash: String)(action: => Future[T], logSuccess: String, logFailure: String): Future[Option[T]] = {
    userService.findUserByPasswordHash(userHash).flatMap {
      case Some(_) =>
        logger.info(logSuccess)
        action.map(Some(_))
      case None =>
        logger.info(logFailure)
        Future.successful(None)
    }
  }

  def subscribeToShow(subscription: SubscriptionEntry): Future[Option[SubscriptionEntry]] =
    processUserRequest(subscription.userHash)(
      subscriptionService.subscribeToShow(subscription),
      s"Subscribing ${subscription.userHash} to ${subscription.imdbId}",
      s"Unknown user tried to subscribe."
    )

  def unsubscribeFromShow(userHash: String, imdbId: String): Future[Option[String]] =
    processUserRequest(userHash)(
      subscriptionService.unsubscribeFromShow(userHash, imdbId),
      s"Unsubscribing $userHash from $imdbId",
      s"Unknown user tried to unsubscribe."
    )

  def getUserFeed(userHash: String): Future[Option[List[EpisodeEntry]]] =
    processUserRequest(userHash)(
      {
        subscriptionService.getUserSubscriptions(userHash).flatMap { subs =>
          val episodes = subs.map { sub =>
            episodeService.getEpisodesWithTorrents(sub.imdbId, sub.startWithSeason, sub.startWithEpisode)
          }
          Future.foldLeft(episodes)(List.empty[EpisodeEntry])(_ ++ _)
        }
      },
      s"Getting feed for $userHash",
      s"Unknown user tried to fetch feed"
    )

  def getEpisodesWithoutTorrents: Future[Seq[EpisodeEntry]] = {
    logger.info("Getting all episodes without torrents")
    episodeService.getAiredEpisodesWithoutTorrents
  }

  def getShowByImdbId(imdbId: String): Future[Option[ShowEntry]] = {
    logger.info(s"Getting show entry for $imdbId")
    showService.getShowByImdbId(imdbId)
  }

  def getShowByTvdbId(tvdbId: Int): Future[Option[ShowEntry]] = {
    logger.info(s"Getting show entry for $tvdbId")
    showService.getShowByTvdbId(tvdbId)
  }


  def getShowTitleByImdbId(imdbId: String): Future[String] = showService.getShowByImdbId(imdbId).map {
    case Some(show) => show.title
    case None => "<unknown>"
  }

  def getShowTitleByTvdbId(tvdbId: Int): Future[String] = showService.getShowByTvdbId(tvdbId).map {
    case Some(show) => show.title
    case None => "<unknown>"
  }

  def updateEpisode(episodeEntry: EpisodeEntry): Future[Option[EpisodeEntry]] = {
    logger.info(s"Saving torrent for ${episodeEntry.imdbId}, s${episodeEntry.season}e${episodeEntry.episode}")
    episodeService.updateEpisode(episodeEntry)
  }

  def addShow(showEntry: ShowEntry): Future[Option[ShowEntry]] = {
    logger.info(s"Saving show entry: $showEntry")
    showService.addShow(showEntry)
  }

  def addEpisode(episodeEntry: EpisodeEntry): Future[Option[EpisodeEntry]] = {
    logger.info(s"Saving episode entry: $episodeEntry")
    episodeService.addEpisode(episodeEntry)
  }
}
