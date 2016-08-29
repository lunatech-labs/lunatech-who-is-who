package models

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

case class DBPerson(id: Long, name: String, email: String, locationID: Long , photo: String, description: String)

object DBPerson {
  implicit val personFormatF = Json.format[Person]
}
