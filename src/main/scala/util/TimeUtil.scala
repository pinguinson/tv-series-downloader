package util

import java.time.LocalDate.{now, parse}

/**
  * Created by pinguinson on 5/6/2017.
  */
object TimeUtil {
  def today: String = now.toString
  def shouldBeReleased(date: String): Boolean = now.compareTo(parse(date)) >= 0
}
