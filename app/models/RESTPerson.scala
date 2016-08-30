package models

import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

//case class Person(id: Long, name: String, email: String, photo: String, description: String)
case class RESTPerson(name: String, email: String, description: String)

object RESTPerson {
  //  implicit val personFormatR = Json.reads[Person]
  //  implicit val personFormatW = Json.writes[Person]
    implicit val RESTPersonFormat = Json.format[RESTPerson]


  def fromPerson(person: Person) = new RESTPerson(person.name, person.email, person.description)

  def doSomething (rESTPerson: RESTPerson) ={
    println("we got this object in json rest request:")
    println(rESTPerson.toString)
  }


//  implicit val personRead :Reads[Person]= (
//      (JsPath \ "name").read[String] and
//      (JsPath \ "email").read[String] and
//      (JsPath \ "description").read[String]
//    )(Person.apply _)
}
