package models

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

case class Person(id: Long, name: String, email: String, location: Office , photo: String, description: String)
//case class PersonJson(name: String, email: String, description: String)

object Person {
//  implicit val personFormatR = Json.reads[Person]
//  implicit val personFormatW = Json.writes[Person]
  implicit val personFormatF = Json.format[Person]


//  implicit val personWrite :Writes[Person]= (
//    (JsPath \ "idManual").write[Long] and
//    (JsPath \ "nameManual").write[String] and
//      (JsPath \ "emailManual").write[String] and
//      (JsPath \ "photoManual").write[String] and
//      (JsPath \ "descriptionManual").write[String]
//    )(unlift(Person.unapply))
//
//
//  implicit val personRead :Reads[Person]= (
//    (JsPath \ "idManual").read[Long] and
//      (JsPath \ "nameManual").read[String] and
//      (JsPath \ "emailManual").read[String] and
//      (JsPath \ "photoManual").read[String] and
//      (JsPath \ "descriptionManual").read[String]
//    )(Person.apply _)

}
