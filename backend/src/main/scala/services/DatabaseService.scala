package services

import akka.dispatch.MessageDispatcher
import models.entries._
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable
import slick.util.Logging

import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by pinguinson on 5/5/2017.
  */
class DatabaseService(dbConfigName: String)(implicit executionContext: ExecutionContext) extends Logging {
  private implicit val db: PostgresProfile.backend.Database = Database.forConfig(dbConfigName)
  private val episodeService = new EpisodeService
  private val showService = new ShowService
  private val subscriptionService = new SubscriptionService
  private val userService = new UserService
  private val authService = new AuthService

  import episodeService.episodes
  import showService.shows
  import subscriptionService.subscriptions
  import userService.users
  import authService.tokens

  private val tables = List(episodes, shows, subscriptions, users, tokens)

  /** Creates missing tables */
  def createSchemaIfNotExists: Future[Unit] = {
    println("creating tables")
    val existing = db.run(MTable.getTables)
    existing.flatMap { v =>
      println("creating tables (inside flatmap)")
      val names = v.map(_.name.name)
      println(names)
      val missingTables = tables.filterNot(table => names.contains(table.baseTableRow.tableName))
      val createIfNotExist = missingTables.map { table =>
        println(s"Table ${table.baseTableRow.tableName} is missing, creating...")
        table.schema.create
      }
      db.run(DBIO.seq(createIfNotExist: _*))

    }

  }

  def getNewImdbIds: Future[Seq[String]] = {
    val query = for {
      (sub, show) <- subscriptions joinLeft shows on (_.imdbId === _.imdbId)
      if show.isEmpty
    } yield sub.imdbId

    db.run(query.result)
  }

  private def processUserRequest[T](userEntry: UserEntry)(action: => Future[T], logSuccess: String, logFailure: String): Future[Option[T]] = {
    userService.findUserByName(userEntry.username).flatMap {
      case Some(_) =>
        logger.info(logSuccess)
        action.map(Some(_))
      case None =>
        logger.info(logFailure)
        Future.successful(None)
    }
  }

  def subscribeToShow(user: UserEntry, subscription: SubscriptionEntry): Future[Option[SubscriptionEntry]] =
    processUserRequest(user)(
      subscriptionService.subscribeToShow(subscription),
      s"Subscribing ${user.username} to ${subscription.imdbId}",
      s"Unknown user tried to subscribe."
    )

  def unsubscribeFromShow(userEntry: UserEntry, imdbId: String): Future[Option[String]] =
    processUserRequest(userEntry)(
      subscriptionService.unsubscribeFromShow(userEntry.md5, imdbId),
      s"Unsubscribing ${userEntry.username} from $imdbId",
      s"Unknown user tried to unsubscribe."
    )

  def getUserFeed(userEntry: UserEntry): Future[Option[List[EpisodeEntry]]] =
    processUserRequest(userEntry)(
      getUserFeed(userEntry.md5),
      s"Getting feed for ${userEntry.username}",
      s"Unknown user tried to fetch feed"
    )

  def getUserFeed(userHash: String): Future[List[EpisodeEntry]] =
    subscriptionService.getUserSubscriptions(userHash).flatMap { subs =>
      val episodes = subs.map { sub =>
        episodeService.getEpisodesWithTorrents(sub.imdbId, sub.startWithSeason, sub.startWithEpisode)
      }
      Future.foldLeft(episodes)(List.empty[EpisodeEntry])(_ ++ _)
    }

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
    logger.info(s"Updating episode ${episodeEntry.imdbId}, ${episodeEntry.episodeToString}")
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

  // For now using ugly hack: setting `imdbId` to show title
  def getUserShows(userEntry: UserEntry): Future[Option[List[SubscriptionEntry]]] = {
    val smth = db.run((for {
      (sub, show) <- subscriptions.filter(_.userHash === userEntry.md5) joinLeft shows on (_.imdbId === _.imdbId)
    } yield (show.map(_.title), sub)).result)
    smth.map(seq => Some(seq.map(x => x._2.copy(imdbId = x._1.getOrElse(x._2.imdbId))).toList))
  }

  def signIn(userEntry: UserEntry): Future[Option[TokenEntry]] =
    authService.signIn(userEntry)

  def signUp(userEntry: UserEntry): Future[Option[TokenEntry]] =
    authService.signUp(userEntry)

  def authenticate(token: String): Future[Option[UserEntry]] =
    authService.authenticate(token)

}
