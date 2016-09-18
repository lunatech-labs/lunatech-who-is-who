package models

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

case class Person(id: Option[Long], name: String, email: String, role: String, phone: Option[String], location: String , photo: Option[String], description: String)

object Person {
  implicit val formatter = Json.format[Person]
}
