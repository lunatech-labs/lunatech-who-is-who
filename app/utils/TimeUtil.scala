package utils

import org.joda.time.DateTime
import java.util._

object TimeUtil {

  val vacationDays = Seq("01-01", "21-04", "27-04", "29-05", "09-06", "25-12", "26-12")
  /**
   * Converts seconds to hours and round hours to a quarter. This function rounds the hours the same Toggl does by
   * rounding minutes always up to a quarter. Eg. 1h03 and 1h14 will be both rounded to 1h15.
   * @param seconds the seconds to convert
   * @return a float rounded to a quarter, representing the number of hours
   */
  def toHours(seconds: Long) = if (seconds > 0) Math.ceil((seconds.toFloat / 3600) * 4).toFloat / 4f else 0f

  def toDays(hours: Float) = if (hours > 0) Math.ceil((hours / 8) * 4).toFloat / 4f else 0f

  def isVacationDay(day: Calendar) = {
    val fmt = new java.text.SimpleDateFormat("dd-MM")
    vacationDays.contains(fmt.format(day.getTime()))
  }


  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)


  def ordinal(date: Date) = {
  	val cal = Calendar.getInstance()
  	cal.setTime(date)
  	val num = cal.get(Calendar.DAY_OF_MONTH)
  	val suffix = Array("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")
  	val m = num % 100
  	val index = if(m > 10 && m < 20){ 0 } else { (m % 10) }
  	num.toString + suffix(index)
  }

  def getDays(fromDate: Date, untilDate: Date) = {
    val calStart = Calendar.getInstance()
    calStart.setTime(fromDate)
    val calEnd = Calendar.getInstance()
    calEnd.setTime(untilDate)
    calEnd.add(Calendar.DAY_OF_MONTH, 1)
    var days = 0
    while (calStart.before(calEnd)) {
        if ((!isVacationDay(calStart) && calStart.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) && (calStart.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)) {
            days += 1
        }
         calStart.add(Calendar.DAY_OF_MONTH, 1)
    }
    days
}

def md5(s: String) = {
    import java.security.MessageDigest
    MessageDigest.getInstance("MD5").digest(s.getBytes).map("%02X".format(_)).mkString.toLowerCase
}

}
