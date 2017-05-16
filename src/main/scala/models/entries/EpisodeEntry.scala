package models.entries

/**
  * Created by pinguinson on 5/7/2017.
  */
case class EpisodeEntry(imdbId: String, season: Int, episode: Int, airDate: String, filename: Option[String], magnet: Option[String]) {
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
}
