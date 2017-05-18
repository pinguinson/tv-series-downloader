package services

import models.entries.UserEntry
import models.tables.UserTable
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by pinguinson on 5/10/2017.
  */
class UserService(implicit db: PostgresProfile.backend.Database, executionContext: ExecutionContext) {

  val users = TableQuery[UserTable]

  def getAllUsers: Future[Seq[UserEntry]] = db.run(users.result)

  def findUserByName(username: String): Future[Option[UserEntry]] =
  db.run(users.filter(_.username === username).result.headOption)


  def findUserByPasswordHash(md5password: String): Future[Option[UserEntry]] =
    db.run(users.filter(_.md5password === md5password).result.headOption)

  def addUser(user: UserEntry): Future[Option[UserEntry]] = findUserByName(user.username).flatMap {
  case Some(_) => Future.successful(None)
  case None => db.run(users += user).map(_ => Some(user))
}
}