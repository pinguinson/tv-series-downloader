package services

import models.entries.ShowEntry
import models.tables.ShowTable
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by pinguinson on 5/5/2017.
  */
class ShowService(implicit db: PostgresProfile.backend.Database, executionContext: ExecutionContext) {

  val shows = TableQuery[ShowTable]

  def getAllShows: Future[Seq[ShowEntry]] = db.run(shows.result)

  def getShowByImdbId(imdbId: String): Future[Option[ShowEntry]] =
    db.run(shows.filter(_.imdbId === imdbId).result.headOption)

  def getShowByTvdbId(tvdbId: Int): Future[Option[ShowEntry]] =
    db.run(shows.filter(_.tvdbId === tvdbId).result.headOption)

  def getShow(show: ShowEntry): Future[Option[ShowEntry]] =
    getShowByImdbId(show.imdbId)

  def addShow(show: ShowEntry): Future[Option[ShowEntry]] = getShow(show).flatMap {
    case Some(_) => Future.successful(None)
    case None => db.run(shows += show).map(_ => Some(show))
  }
}
