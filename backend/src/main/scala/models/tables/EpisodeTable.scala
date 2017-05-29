package models.tables

import models.entries.EpisodeEntry
import slick.jdbc.PostgresProfile.api._

/**
  * Created by pinguinson on 5/5/2017.
  */
class EpisodeTable(tag: Tag) extends Table[EpisodeEntry](tag, "episodes") {
  def imdbId = column[String]("imdbid")
  def season = column[Int]("season")
  def episode = column[Int]("episode")
  def airdate = column[String]("airdate")
  def filename = column[Option[String]]("filename")
  def magnet = column[Option[String]]("magnet")
  def searches = column[Int]("searches")

  def * = (imdbId, season, episode, airdate, filename, magnet, searches) <> (EpisodeEntry.tupled, EpisodeEntry.unapply)
}

