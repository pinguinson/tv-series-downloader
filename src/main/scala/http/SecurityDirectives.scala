package http

import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.server.Directives._
import models.entries.{TokenEntry, UserEntry}
import services.AuthService

import scala.util.Success

/**
  * Created by pinguinson on 23.05.17.
  */
trait SecurityDirectives {

  protected val authService: AuthService
  val tokenName = "auth_token"

  def expires = Some(DateTime.now + 1000 * 3600 * 24 * 7) // 1 week from now

  def authenticate: Directive1[UserEntry] = {
    optionalCookie(tokenName).flatMap {
      case Some(cookie) =>
        onComplete(authService.authenticate(cookie.value)).flatMap {
          case Success(Some(loggedUser)) => provide(loggedUser)
          case _ => complete(s"Invalid $tokenName")
        }
      case None => complete(s"Cookie $tokenName not found")
    }
  }

  def giveToken(tokenEntry: TokenEntry): Route = {
    setCookie(HttpCookie(
      name = tokenName,
      value = tokenEntry.token,
      path = Some("/"),
      expires = expires
    )) {
      complete(s"Logged in as $tokenEntry")
    }
  }

  def deleteToken(): Route = {
    deleteCookie(tokenName, path = "/") {
      complete {
        "Logged out"
      }
    }
  }
}
