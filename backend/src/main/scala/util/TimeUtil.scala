package util

import com.github.nscala_time.time.Imports._

/**
  * Created by pinguinson on 5/6/2017.
  */
object TimeUtil {
  def today: String = DateTime.now.toString("yyyy-MM-dd")
  def weekAgo: String = DateTime.lastWeek.toString("yyyy-MM-dd")
}
