package dal

import java.io.File
import javax.inject.{Inject, Singleton}

import models.{DBPerson, Office, Person}
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
class PersonRepository @Inject()(officesRepo: OfficeRepository, protected  val dbConfigProvider: DatabaseConfigProvider) extends OfficeComponent with HasDatabaseConfigProvider[JdbcProfile]{
  // We want the JdbcProfile for this provider
  //  val officesRepo: OfficeRepository
  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import driver.api._

  /**
    * Here we define the table. It will have a name of people
    */
  private class PeopleTable(tag: Tag) extends Table[DBPerson](tag, "people") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def name = column[String]("name")
    def email = column[String]("email")
    def location = column[Long]("location")
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
    def * = (id, name, email , location, photo, description) <> ((DBPerson.apply _).tupled, DBPerson.unapply)
  }

  /**
    * The starting point for all queries on the people table.
    */
  private val people = TableQuery[PeopleTable]
  private val offices = TableQuery[OfficesTable]

  /**
    * Create a person with the given name and ....
    *
    * This is an asynchronous operation, it will return a future of the created person, which can be used to obtain the
    * id for that person.
    */
  def create_dbperson(name: String, email: String, location: Long, photo: String, description: String): Future[DBPerson] = db.run {
    //    Logger.debug("debug ---10---")
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    (people.map(p => (p.name, p.email, p.location, p.photo, p.description))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning people.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((nameInfo, id) => DBPerson(id, nameInfo._1, nameInfo._2, nameInfo._3, nameInfo._4, nameInfo._5))
      // And finally, insert the person into the database
      ) += (name, email, location, photo, description)
  }
  def create_person(name: String, email: String, location: String, photo: String, description: String): Future[Person] =  {
    create_dbperson(name, email, location.toLong , photo, description).map { dbperson =>
      Logger.debug(dbperson.toString)
      val myoffice = Office(dbperson.locationID,"","","","")
      val myperson = Person(dbperson.id,dbperson.name,dbperson.email,myoffice,dbperson.photo,dbperson.description)
      //Logger.debug("DEBUG " + myperson.toString)
      myperson
    }
  }
  def givemeobject(iPerson: DBPerson) : Future[Option[Person]] = {
    officesRepo.list().map {
      p => {
        p.find (r => r.id == iPerson.locationID).map{ office =>
          Person(iPerson.id,iPerson.name,iPerson.email,office,iPerson.photo,iPerson.description)
        }
      }
    }
  }

  // this is another implemetation (using query join)
  def listPersons_merge(): Future[Seq[Person]] = {
    //Logger.debug("Debug: ----e101--")
    val query = for {
      p <- people
      o <- offices if o.id === p.location
    } yield (p, o)
    Logger.debug(query.result.statements.toString())
    db.run(query.result).map(rows => rows.map {
      case (p, o) => Person(p.id, p.name, p.email  ,o, p.photo , p.description )
    })

  }

  /**
    * List all the people in the database.
    */
  def listDBPersons(): Future[Seq[DBPerson]] =  {
    val abc = db.run(people.result)
    //Logger.debug(abc.toString)
    abc
  }
  // this is the first implementation using two separate queries
  def listPersons(): Future[Seq[Person]] = {
    listDBPersons().flatMap {
      dbPersons => {
        val persons: Seq[Future[Option[Person]]] = dbPersons.map(one => givemeobject(one))
        val res: Future[Seq[Option[Person]]] = Future.sequence(persons)
        val list: Future[Seq[Person]] = res.map(persons => persons.flatten)
        list
      }
    }
  }

  def checkEmails(iEmail: String): Future[Option[Person]] =  {
    val dbperson = db.run(people.filter(_.email === iEmail).result.headOption).flatMap {
      case Some(ppp) => givemeobject(ppp)
      case None => Future(None)}
    dbperson
  }

  def checkEmails(iEmail: String, iId: Long): Future[Option[Person]] =  {
    db.run(people.filter(_.email === iEmail).filter(_.id =!= iId).result.headOption).flatMap {
      case Some(ppp) => givemeobject(ppp)
      case None => Future(None)
    }
  }
  def get(id: Long): Future[Option[Person]] =  {
    //Logger.debug("ID = " + id)
    db.run(people.filter(_.id === id).result.headOption).flatMap {
      case Some(ppp) => givemeobject(ppp)
      case None => Future(None)
    }
  }

  def getByEmail(email: String): Future[Option[Person]] =  {
    //Logger.debug("email = " + email)
    db.run(people.filter(_.email === email).result.headOption).flatMap {
      case Some(ppp) => givemeobject(ppp)
      case None => Future(None)
    }
  }

  // could be written like this:
  //
  def deleteRow(iId: Long): Future[Int] = {
    val action = people.filter(_.id === iId).delete
    Logger.debug(action.statements.head)
    db.run(action)
  }

  def updatePerson(iId: Long, name: String, email: String, locationID: Long, photo: String, description: String, isetphoto: Boolean): Future[Int] = {
    if (isetphoto) {
      get(iId).map {
        case Some(person) => {
          val fullfilename = play.Play.application.configuration.getString("pictures_path") + person.photo
          val file = new File(fullfilename)
          file.delete()
          //Logger.debug("updatePerson - deleting: " + fullfilename)
        }
        case None => Logger.debug("updatePerson - Person Not Found")
      }
      val q = for {c <- people if c.id === iId} yield (c.name, c.email, c.location ,c.description, c.photo)
      val updateAction = q.update(name, email, locationID,description, photo)
      val sql = q.updateStatement
      Logger.debug(sql)
      db.run(updateAction)
    }
    else {
      val q = for {c <- people if c.id === iId} yield (c.name, c.email, c.location ,c.description)
      val updateAction = q.update(name, email, locationID,description)
      val sql = q.updateStatement
      Logger.debug(sql)
      db.run(updateAction)
    }
  }
}
