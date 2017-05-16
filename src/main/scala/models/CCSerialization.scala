package models

import actors.TorrentActor.{Token => RarbgToken, _}
import actors.TvdbActor.{Token => TvdbToken, _}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import models.entries.SubscriptionEntry
import spray.json.DefaultJsonProtocol

/**
  * Created by pinguinson on 5/10/2017.
  */
object CCSerialization extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val credentialsFormat = jsonFormat3(Credentials)
  implicit val tvdbTokenFormat = jsonFormat1(TvdbToken)
  implicit val rarbgTokenFormat = jsonFormat1(RarbgToken)
  implicit val episodeShortFormat = jsonFormat4(TvdbEpisode)
  implicit val showResponseFormat = jsonFormat1(GetShowInfoResponse)
  implicit val showSearchEntryFormat = jsonFormat2(ShowSearchEntry)
  implicit val findShowResponseFormat = jsonFormat1(TvdbFindShowEntry)
  implicit val showEpisodesFormat = jsonFormat2(ShowEpisodes)
  implicit val subscriptionEntryFormat = jsonFormat5(SubscriptionEntry)

  implicit val episodeInfoFormat = jsonFormat4(EpisodeInfo)
  implicit val episodeTorrentFormat = jsonFormat4(EpisodeTorrent)
  implicit val rarbgResponseFormat = jsonFormat1(RarbgResponse)
}
