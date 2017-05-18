package services

import models.entries.EpisodeEntry
import models.tables.EpisodeTable
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import util.TimeUtil._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by pinguinson on 5/5/2017.
  */
class EpisodeService(implicit db: PostgresProfile.backend.Database, executionContext: ExecutionContext) {

  val episodes = TableQuery[EpisodeTable]

  def getAllEpisodes: Future[Seq[EpisodeEntry]] =
    db.run(episodes.result)

  def getEpisode(episode: EpisodeEntry): Future[Option[EpisodeEntry]] = {
    db.run(episodes.filter(e =>
      e.imdbId === episode.imdbId &&
        e.season === episode.season &&
        e.episode === episode.episode
    ).result.headOption)
  }

  def addEpisode(episode: EpisodeEntry): Future[Option[EpisodeEntry]] = getEpisode(episode).flatMap {
    case Some(_) => Future.successful(None)
    case None => db.run(episodes += episode).map(_ => Some(episode))
  }

  def updateEpisode(updated: EpisodeEntry): Future[Option[EpisodeEntry]] = getEpisode(updated).flatMap {
    case Some(_) => db.run(episodes.filter(e =>
      e.imdbId === updated.imdbId &&
        e.season === updated.season &&
        e.episode === updated.episode
    ).update(updated)).map(_ => Some(updated))
    case None => Future.successful(None)
  }

  def getAiredEpisodesWithoutTorrents: Future[Seq[EpisodeEntry]] =
    db.run(episodes.filter(e =>
      e.magnet.isEmpty &&
        e.airdate <= today &&
        // ignore old episodes with no torrents
        !(e.airdate <= yearAgo && e.searches >= 1) &&
        // ignore special episodes
        e.season >= 1
    ).result)

  def getEpisodesWithTorrents(imdbId: String, startWithSeason: Int, startWithEpisode: Int): Future[List[EpisodeEntry]] =
    db.run(episodes.filter(e =>
      e.imdbId === imdbId && e.magnet.isDefined &&
        (e.season > startWithSeason || (e.season === startWithSeason && e.episode >= startWithEpisode))
    ).result).map(_.toList)
}
