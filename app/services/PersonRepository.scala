package services

import java.io.File
import javax.inject.{Inject, Singleton}

import models._
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
  * A repository for people.
  *
  * @param dbConfigProvider The Play db config provider. Play will inject this for you.
  */
@Singleton
class PersonRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile]{
  // We want the JdbcProfile for this provider
  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import driver.api._

  /**
    * Here we define the table. It will have a name of people
    */
  private class PeopleTable(tag: Tag) extends Table[Person](tag, "people") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def name = column[String]("name")
    def email = column[String]("email")
    def role = column[String]("role")
    def phone = column[String]("phone")
    def location = column[String]("location")
    def photo = column[String]("photo")
    def description = column[String]("description")
    /**
      * This is the tables default "projection".
      *
      * It defines how the columns are converted to and from the Person object.
      *
      * In this case, we are simply passing the id, name and page parameters to the Person case classes
      * apply and unapply methods.
      */
    def * = (id, name, email, role, phone, location, photo, description).shaped <> (
      { case (id, name, email, role, phone, location, photo, description) =>
        Person(Option(id), name, email, role, Option(phone), location, Option(photo), description)
      }, {
        p: Person => Some((p.id.getOrElse(-1L), p.name, p.email, p.role, p.phone.getOrElse(""), p.location, p.photo.getOrElse(""), p.description))
      })
  }

  /**
    * The starting point for all queries on the people table.
    */
  private val people = TableQuery[PeopleTable]

  /**
    * Create a person with the given name and ....
    *
    * This is an asynchronous operation, it will return a future of the created person, which can be used to obtain the
    * id for that person.
    */
  def insert(person: Person): Future[Person] = {
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    val insertQuery = people returning people.map(_.id) into ((p, id) => p.copy(id = Some(id)))
    val action = insertQuery += person
    db.run(action)
  }

  /**
    * List all the people in the database.
    */
  def all(): Future[Seq[Person]] = {
    db.run(people.sortBy(_.name.asc.nullsFirst).result)
  }

  def findByLocation(location: String): Future[Seq[Person]] = {
    db.run(people.filter(_.location === location).sortBy(_.name.asc.nullsFirst).result)
  }

  def countByLocation(location: String): Future[Int] = {
    db.run(people.filter(_.location === location).length.result)
  }

  def count(): Future[Int] = {
    db.run(people.length.result)
  }

  def findById(id: Long): Future[Option[Person]] = {
    //Logger.debug("ID = " + id)
    db.run(people.filter(_.id === id).result.headOption)
  }

  // could be written like this:
  //
  def delete(id: Long): Future[Int] = {
//    println("DEBUG 30 deleting: " + id.toString)
    val action = people.filter(_.id === id).delete
    db.run(action)
  }

  def updatePhoto(personId: Long, photo: String): Future[Int] = {
    val q = for {c <- people if c.id === personId} yield (c.photo)
    val updateAction = q.update(photo)
    val sql = q.updateStatement
    db.run(updateAction)
  }

  def update(person: Person): Future[Int] = {
    val q = for {c <- people if c.id === person.id} yield (c.name, c.email, c.role, c.phone, c.location, c.description)
    val updateAction = q.update(person.name, person.email, person.role, person.phone.getOrElse(""), person.location, person.description)
    val sql = q.updateStatement
    db.run(updateAction)
  }
}
