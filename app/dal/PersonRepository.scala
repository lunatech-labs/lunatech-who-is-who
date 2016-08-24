package dal

import java.io.File
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import models.Person

import scala.concurrent.{Future, ExecutionContext}

/**
  * A repository for people.
  *
  * @param dbConfigProvider The Play db config provider. Play will inject this for you.
  */
@Singleton
class PersonRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import driver.api._

  /**
    * Here we define the table. It will have a name of people
    */
  private class PeopleTable(tag: Tag) extends Table[Person](tag, "people") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def name = column[String]("name")

    /** The age column */
    def email = column[String]("email")

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
    def * = (id, name, email, photo, description) <> ((Person.apply _).tupled, Person.unapply)
  }

  /**
    * The starting point for all queries on the people table.
    */
  private val people = TableQuery[PeopleTable]

  /**
    * Create a person with the given name and age.
    *
    * This is an asynchronous operation, it will return a future of the created person, which can be used to obtain the
    * id for that person.
    */
  def create(name: String, email: String, photo: String, description: String): Future[Person] = db.run {
    //    println("debug ---10---")
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    (people.map(p => (p.name, p.email, p.photo, p.description))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning people.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((nameAge, id) => Person(id, nameAge._1, nameAge._2, nameAge._3, nameAge._4))
      // And finally, insert the person into the database
      ) += (name, email, photo, description)
  }

  /**
    * List all the people in the database.
    */
  def list(): Future[Seq[Person]] = db.run {
    people.result
  }

  def checkEmails(iEmail: String): Future[Option[Person]] = db.run {
    people.filter(_.email === iEmail).result.headOption
  }

  def checkEmails(iEmail: String, iId: Long): Future[Option[Person]] = db.run {
    people.filter(_.email === iEmail).filter(_.id =!= iId).result.headOption
  }

  def get(id: Long): Future[Option[Person]] = db.run {
    println("ID = " + id)
    people.filter(_.id === id).result.headOption
  }

  def getByEmail(email: String): Future[Option[Person]] = db.run {
    println("email = " + email)
    people.filter(_.email === email).result.headOption
  }

  // could be written like this:
  //
  def deleteRow(iId: Long): Future[Int] = {
    val action = people.filter(_.id === iId).delete
    println(action.statements.head)
    //    val q =for { c <- people if c.id === iId } yield (c.name, c.email, c.description, c.photo)
    //val sql = q.updateStatement
    //println(sql)
    db.run(action)
  }

  def updatePerson(iId: Long, name: String, email: String, photo: String, description: String, isetphoto: Boolean): Future[Int] = {
    if (isetphoto) {
      get(iId).map {
        case Some(person) => {
          val fullfilename = play.Play.application.configuration.getString("pictures_path") + person.photo
          val file = new File(fullfilename)
          file.delete()
          println("updatePerson - deleting: " + fullfilename)
        }
        case None => println("updatePerson - Person Not Found")

      }

      val q = for {c <- people if c.id === iId} yield (c.name, c.email, c.description, c.photo)
      val updateAction = q.update(name, email, description, photo)
      val sql = q.updateStatement
      println(sql)
      db.run(updateAction)
    }
    else {
      val q = for {c <- people if c.id === iId} yield (c.name, c.email, c.description)
      val updateAction = q.update(name, email, description)
      val sql = q.updateStatement
      println(sql)
      db.run(updateAction)
    }
  }


}
