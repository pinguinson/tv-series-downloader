package models.tables

import models.entries.SubscriptionEntry
import slick.jdbc.PostgresProfile.api._
/**
  * Created by pinguinson on 5/10/2017.
  */
class SubscriptionTable(tag: Tag) extends Table[SubscriptionEntry](tag, "usershows") {
  def userHash = column[String]("userhash")
  def imdbId = column[String]("imdbid")
  def startWithSeason = column[Int]("startwithseason")
  def startWithEpisode = column[Int]("startwithepisode")
  def watching = column[Boolean]("watching")

  def * = (userHash, imdbId, startWithSeason, startWithEpisode, watching) <> (SubscriptionEntry.tupled, SubscriptionEntry.unapply)
}

