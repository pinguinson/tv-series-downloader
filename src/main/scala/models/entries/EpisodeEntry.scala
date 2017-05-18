package models.entries

/**
  * Created by pinguinson on 5/7/2017.
  */
case class EpisodeEntry(imdbId: String, season: Int, episode: Int, airDate: String, filename: Option[String], magnet: Option[String], searches: Int) {
  def episodeToString: String = {
    f"s$season%02de$episode%02d"
  }

  def toXml: scala.xml.Elem = {
    <item>
      <title>{s"$imdbId $episodeToString"}</title>
      <link>{magnet.getOrElse("<unknown>")}</link>
      <description>{filename.getOrElse("<unknown>")}</description>
    </item>
  }

  private def torrentToString = {
    filename match {
      case Some(_) => "has torrent"
      case None => "no torrent"
    }
  }

  override def toString = {
    s"$imdbId $episodeToString $airDate $torrentToString $searches"
  }
}
