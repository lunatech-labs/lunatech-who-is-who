package models

import play.api.libs.json._

case class Person(id: Long, name: String, email: String, photo: String, description: String)

object Person {
  //println("debug ---20---")
  implicit val personFormat = Json.format[Person]
  //println("debug ---21--- ")
}
