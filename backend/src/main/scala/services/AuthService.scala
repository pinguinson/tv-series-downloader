package services

import models.entries.{TokenEntry, UserEntry}
import models.tables.{TokenTable, UserTable}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import util.MD5.hash

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by pinguinson on 23.05.17.
  */
class AuthService(implicit db: PostgresProfile.backend.Database, executionContext: ExecutionContext) {
  val tokens = TableQuery[TokenTable]
  val users = TableQuery[UserTable]

  def signIn(userEntry: UserEntry): Future[Option[TokenEntry]] = {
    db.run(users.filter(_.username === userEntry.username).result).flatMap { users =>
      users.find(_.md5password == userEntry.md5password) match {
        case Some(user) => db.run(tokens.filter(_.username === user.username).result.headOption).flatMap {
          case Some(token) => Future.successful(Some(token))
          case None        => createToken(user).map(token => Some(token))
        }
        case None => Future.successful(None)
      }
    }
  }

  def signUp(userEntry: UserEntry): Future[Option[TokenEntry]] =
    db.run(users.filter(_.username === userEntry.username).result.headOption).flatMap {
      case Some(_) =>
        Future.successful(None)
      case None =>
        db.run(users += userEntry).flatMap(_ => createToken(userEntry)).map(Some(_))
    }

  def authenticate(token: String): Future[Option[UserEntry]] =
    db.run((for {
      token <- tokens.filter(_.token === token)
      user <- users.filter(_.username === token.username)
    } yield user).result.headOption)

  def createToken(userEntry: UserEntry): Future[TokenEntry] =
    db.run(tokens returning tokens += TokenEntry(username = userEntry.username))
}
