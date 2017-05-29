package models.tables

import models.entries.TokenEntry
import slick.jdbc.PostgresProfile.api._
/**
  * Created by pinguinson on 23.05.17.
  */
class TokenTable(tag: Tag) extends Table[TokenEntry](tag, "tokens") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username")
  def token = column[String]("token")

  def * = (id, username, token) <> (TokenEntry.tupled, TokenEntry.unapply)
}
