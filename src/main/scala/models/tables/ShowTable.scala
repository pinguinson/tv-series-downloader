package models.tables

import models.entries.ShowEntry
import slick.jdbc.PostgresProfile.api._
/**
  * Created by pinguinson on 5/10/2017.
  */
class ShowTable(tag: Tag) extends Table[ShowEntry](tag, "showids") {
  def imdbId = column[String]("imdbid")
  def tvdbId = column[Int]("tvdbid")
  def title = column[String]("title")

  def * = (imdbId, tvdbId, title) <> (ShowEntry.tupled, ShowEntry.unapply)
}
