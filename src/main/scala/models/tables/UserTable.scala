package models.tables

import models.entries.UserEntry
import slick.jdbc.PostgresProfile.api._

/**
  * Created by pinguinson on 5/10/2017.
  */
class UserTable(tag: Tag) extends Table[UserEntry](tag, "users") {
  def username = column[String]("username")
  def md5password = column[String]("md5password")

  def * = (username, md5password) <> (UserEntry.tupled, UserEntry.unapply)
}
