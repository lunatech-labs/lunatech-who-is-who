package dal

import javax.inject.{Inject, Singleton}

import models.Office
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Map
import scala.concurrent.{ExecutionContext, Future}

trait OfficeComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  class OfficesTable(tag: Tag) extends Table[Office](tag, "offices") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey)

    /** The name column */
    def name = column[String]("name")

    /** The age column */
    def city = column[String]("city")

    def country = column[String]("country")

    def remarks = column[String]("remarks")

    /**
      * This is the tables default "projection".
      *
      * It defines how the columns are converted to and from the Person object.
      *
      * In this case, we are simply passing the id, name and page parameters to the Person case classes
      * apply and unapply methods.
      */
    def * = (id, name, city, country, remarks) <> ((Office.apply _).tupled, Office.unapply)
  }

}
/**
  * A repository for people.
  *
  * @param dbConfigProvider The Play db config provider. Play will inject this for you.
  */
@Singleton
class OfficeRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends OfficeComponent with HasDatabaseConfigProvider[JdbcProfile]{
  // We want the JdbcProfile for this provider


  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import driver.api._

  /**
    * Here we define the table. It will have a name of people
    */


  /**
    * The starting point for all queries on the people table.
    */
  private val offices = TableQuery[OfficesTable]
  /**
    * List all the people in the database.
    */
  def list(): Future[Seq[Office]] = db.run {
    val example = Map("1" -> "Paris", "2" -> "Rotterdam")
    offices.result
  }
//  def listMap(): Future[Map[String,String]] = db.run {
  def listMap(): Future[Map[String,String]]  =  {
    val example = Map("1" -> "Paris", "2" -> "Rotterdam")

    println("start listMap")
    println("----1---")
    val seqresult = db.run(offices.result)
    println("----2---")
//    val a:Future[List[Office]] = seqresult
    val mapResult = scala.collection.immutable.Map[String, String]()
    seqresult.map {officeseq => if (officeseq.isEmpty) {println("DEBUG EMPTY ----100----- ")
      // some other stuff
    }}
    //seqresult.map {officeseq => officeseq.foreach { office => println("DEBUG office.name = "+ office.name) } }
//    seqresult.map {officeseq => officeseq.foreach { office => mapResult += (office.id.toString -> office.name) }}
//    println("-----actual---("+mapResult("1")+")---END)")

    val res = seqresult.map {
      officeSeq =>
        officeSeq.map(office => (office.id.toString, office.name)).toMap
    }
    println("----3---")


    res
  }

  def listM(): Future[Map[String, String]] = db.run(offices.result).map(_.map(office => (office.id.toString, office.name)).toMap)
}
