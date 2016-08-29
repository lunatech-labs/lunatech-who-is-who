package models

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

case class Office(id: Long, name: String, city: String, country: String, remarks: String)

object Office {
  implicit val officeFormat = Json.format[Office]
}
