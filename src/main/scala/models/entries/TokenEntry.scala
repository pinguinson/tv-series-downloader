package models.entries

/**
  * Created by pinguinson on 23.05.17.
  */
import java.util.UUID

case class TokenEntry(id: Option[Long] = None, username: String, token: String = UUID.randomUUID().toString.replaceAll("-", ""))

