package services

import models.entries.SubscriptionEntry
import models.tables.SubscriptionTable
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by pinguinson on 5/5/2017.
  */
class SubscriptionService(implicit db: PostgresProfile.backend.Database, executionContext: ExecutionContext) {

  private val subscriptions = TableQuery[SubscriptionTable]

  def getAllSubscriptions: Future[Seq[SubscriptionEntry]] = db.run(subscriptions.result)

  def getUserSubscriptions(userHash: String): Future[List[SubscriptionEntry]] =
    db.run(subscriptions.filter(_.userHash === userHash).result).map(_.toList)

  def subscribeToShow(subscription: SubscriptionEntry): Future[SubscriptionEntry] = {
    getUserSubscriptions(subscription.userHash).flatMap {
      case l: Seq[SubscriptionEntry] if l.exists(_.imdbId == subscription.imdbId) =>
        db.run(subscriptions.filter(s =>
          s.userHash === subscription.userHash &&
            s.imdbId === subscription.imdbId
        ).update(subscription).map(_ => subscription))
      case _ =>
        db.run(subscriptions += subscription).map(_ => subscription)
    }
  }

  def unsubscribeFromShow(userHash: String, imdbId: String): Future[String] = {
    getUserSubscriptions(userHash).flatMap {
      case l: Seq[SubscriptionEntry] if l.exists(_.imdbId == imdbId) =>
        db.run(subscriptions.filter(s =>
          s.userHash === userHash &&
            s.imdbId === imdbId
        ).delete.map(_ => "Successfully unsubscribed from show"))
      case _ =>
        Future.successful("You weren't subscribed to this show")
    }
  }
}
