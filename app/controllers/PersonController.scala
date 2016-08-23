package controllers

import java.io.File
import javax.inject._

import dal.PersonRepository
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class PersonController @Inject()(repo: PersonRepository, val messagesApi: MessagesApi)
                                (implicit ec: ExecutionContext) extends Controller with I18nSupport {

  /**
    * The mapping for the person form.
    */
  val personForm: Form[CreatePersonForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "email" -> email,
      "description" -> text,
      "changePhoto" -> boolean
    )(CreatePersonForm.apply)(CreatePersonForm.unapply)
  }
  /**
    * The index action.
    */
  def index = Action.async {
    println()
    repo.list().map(people =>
      Ok(views.html.index(personForm, people)))
  }



  def getDetailedPerson(id: Long) = Action.async {
    println("Debug: getDetailedPerson start here ")
    repo.get(id).map {
      case Some(person) => Ok(views.html.detailedPerson(person))
      case None => NotFound("Person Not Found")
    }
  }
  def editPerson(iId: Long) = Action.async { implicit request =>
    println("Debug: editPerson start")
    repo.get(iId).map {
      case Some(person) =>
        val p = CreatePersonForm(person.name, person.email, person.description, false )
        Ok(views.html.editPerson(personForm.fill(p) , iId))
      case None => NotFound("Person Not Found")
    }
  }

  def deletePerson(id: Long) = Action.async {
    repo.deleteRow(id).map {number =>
      if(number > 0) {
        println("DEBUG: reload index after delete --1--")
        Redirect(routes.PersonController.index())
      }
      else {
        println("DEBUG: faile rrow does not exist")
        Redirect(routes.PersonController.index())
      }
    }
  }
  def save (iId: Long) = Action.async(parse.multipartFormData) { implicit request =>
    println("Debug: start save")
    personForm.bindFromRequest.fold(
      errorForm => {
        println("Debug: Seving edited person")

        repo.list().map(people =>
          Ok(views.html.editPerson(errorForm, iId: Long)))
      },
      person => {
        repo.checkEmails(person.email, iId).flatMap {
        case Some(existingEmail) => {
          println("DEBUG: from edit:  email already exists")
          Future.successful(NotFound("from edit:  email already exists"))
        }

        case None => {
          println("DEBUG: from edit: New person. setimage = " + person.setphoto)

          val temp = if (person.setphoto)
            uploadFile(request, person.setphoto)
          else
            ""

          repo.updatePerson(iId: Long , person.name, person.email, temp, person.description, person.setphoto).map { number =>
            if (number == 1) {
              println("DEBUG: succeed to update " + number)

              Redirect(routes.PersonController.index())
            }
            else
            {
              println("DEBUG: faile to update " + number)

              Redirect(routes.PersonController.index())
            }
          }
            // If successful, we simply redirect to the index page.
           // Redirect(routes.PersonController.index())
        //  }
          //            Ok("Person Added")
//          Future.successful(Redirect(routes.PersonController.index()))
        }
      }}
    )
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
        // check if person already exists
        repo.checkEmails(person.email).flatMap {
          case Some(existingEmail) => {
            println("DEBUG: email already exists")
            Future.successful(NotFound("email already exists"))
          }

          case None => {
            println("DEBUG: New person")

            val temp = uploadFile(request)
            repo.create(person.name, person.email, temp, person.description).map { person =>
              println(person.toString)
              // If successful, we simply redirect to the index page.
              Redirect(routes.PersonController.index())
            }
//            Ok("Person Added")
          }
        }
//        val temp = uploadFile(request)
//        repo.create(person.name, person.email, temp, person.description).map { person =>
//          println(person.toString)
//          // If successful, we simply redirect to the index page.
//          Redirect(routes.PersonController.index())
//        }

      }
    )
  }

  def uploadFile(request: Request[MultipartFormData[Files.TemporaryFile]], iSetPhoto: Boolean = true): String = {
    println("Request is: " +request.toString());

    request.body.file("photo").map { picture =>
      if(!picture.filename.isEmpty) {

        val filenameToUpload = play.Play.application.configuration.getString("pictures_path") + picture.filename

        val file = new File(filenameToUpload)
        picture.ref.moveTo(file)
        picture.filename
      }
      else "ali.jpeg"
    }.getOrElse("ali.jpeg")
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


  def giveMePicture(name: String) = Action {
    val myFile: File =
    if (!(name == "ali.jpeg" || name == ""))
      new File(play.Play.application.configuration.getString("pictures_path") + name)
    else
      new File("public/images/ali.jpeg")
    Ok.sendFile(myFile)
  }

}

/**
  * The create person form.
  *
  * Generally for forms, you should define separate objects to your models, since forms very often need to present data
  * in a different way to your models.  In this case, it doesn't make sense to have an id parameter in the form, since
  * that is generated once it's created.
  */
case class CreatePersonForm(name: String, email: String, description: String, setphoto: Boolean)
