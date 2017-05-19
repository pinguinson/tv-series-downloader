package models.entries

import util.MD5.hash
/**
  * Created by pinguinson on 5/10/2017.
  */
case class UserEntry(username: String, md5password: String) {
  def md5 = hash(username + md5password)
}
