package controllers

import play.api._
import play.api.mvc._
import play.api.i18n._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json.Json
import models._
import dal._

import scala.concurrent.{ExecutionContext, Future}
import javax.inject._

import play.api.libs.Files

class PersonController @Inject()(repo: PersonRepository, val messagesApi: MessagesApi)
                                (implicit ec: ExecutionContext) extends Controller with I18nSupport {

  //not needed.. embedded in play/scala
  def isValidEmail(email: String): Boolean =
  """(\w+)@([\w\.]+)""".r.unapplySeq(email).isDefined

  /**
    * The mapping for the person form.
    */
  val personForm: Form[CreatePersonForm] = Form {
//    println("debug ---4---")
    mapping(
      "name" -> nonEmptyText,
      "email" -> email,
      "description" -> text
    )(CreatePersonForm.apply)(CreatePersonForm.unapply)
  }

  /**
    * The index action.
    */
  def index = Action.async {
//    println("debug ---3---")
    println()
    repo.list().map(people =>
      Ok(views.html.index(personForm, people)))
    //    println("")
  }


  def getDetailedPerson(id: Long) = Action.async {
//    println("debug ---person ID---: " + id.toString)
    //    val person = Person(id,"ali", 10, "ali.jpeg")
    repo.get(id).map {
      case Some(person) => Ok(views.html.detailedPerson(person))
      case None => NotFound("Person Not Found")
    }
  }

  /**
    * The add person action.
    *
    * This is asynchronous, since we're invoking the asynchronous methods on PersonRepository.
    */
  def addPerson = Action.async(parse.multipartFormData) { implicit request =>
    // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle succes.
    personForm.bindFromRequest.fold(
      // The error function. We return the index page with the error form, which will render the errors.
      // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
      // a future because the person creation function returns a future.
      errorForm => {
        repo.list().map(people =>
          Ok(views.html.index(errorForm, people)))
      },
      // There were no errors in the from, so create the person.
      // at this point we have successful person object from html strings name and age
      person => {
        //        println("debug ---6---" + request.body.toString)
        val temp = uploadFile(request)
        repo.create(person.name, person.email, temp, person.description).map { person =>
          println(person.toString)
          // If successful, we simply redirect to the index page.
          Redirect(routes.PersonController.index)
        }

      }
    )
  }

  def uploadFile(request: Request[MultipartFormData[Files.TemporaryFile]]): String = {
    val name = request.body.file("photo").map { picture =>
      import java.io.File
      val filename = picture.filename
      //      println("debug ---101--- " + filename)
      //      println("debug ---102--- " + picture.ref.file.getAbsolutePath)
      val contentType = picture.contentType
      val file = new File(s"public/images/$filename")
      picture.ref.moveTo(file)
      filename
    }
    val n = name.getOrElse("ali.jpeg")
    if (n.isEmpty) "ali.jpeg"
    else n
  }

  /**
    * A REST endpoint that gets all the people as JSON.
    */
  def getPersons = Action.async {
//    println("debug ---1---")
    repo.list().map { people =>
      Ok(Json.toJson(people))
    }
  }

  def getPersons(email: String) = Action.async {
//    println("debug ---1---")
    repo.list().map { people =>
      Ok(Json.toJson(people))
    }
  }
}

/**
  * The create person form.
  *
  * Generally for forms, you should define separate objects to your models, since forms very often need to present data
  * in a different way to your models.  In this case, it doesn't make sense to have an id parameter in the form, since
  * that is generated once it's created.
  */
case class CreatePersonForm(name: String, email: String, description: String)
