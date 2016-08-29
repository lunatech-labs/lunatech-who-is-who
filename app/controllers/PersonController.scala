package controllers

import java.io.File
import javax.inject._

import dal.{OfficeRepository, PersonRepository}
import models.RESTPerson
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.Files
import play.api.libs.json.{JsError, Json}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class PersonController @Inject()(repo: PersonRepository, officesRepo: OfficeRepository, val messagesApi: MessagesApi)
                                (implicit ec: ExecutionContext) extends Controller with I18nSupport {

  var officesMAPCONST:Map[String, String] = Map.empty


  /**
    *
    * The mapping for the person form.
    */
  val personForm: Form[CreatePersonForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "email" -> email,
      "selectLocation" -> text,
      "description" -> text,
      "changePhoto" -> boolean
    )(CreatePersonForm.apply)(CreatePersonForm.unapply)
  }
  /**
    * The index action.
    */
  def index = Action.async {
    println("Debug: calling list map")
    officesRepo.listMap().flatMap( offices => {
      println("Debug: ----d20--")
      repo.listPersons_merge.map(people =>{
        println("Debug: ----d22--")
        for (c <- people)
          println (c.toString)
        Ok(views.html.index(personForm, people, offices ))})})
  }

  // to handle future we need map and async.
  // to handle future future we need flat map.
  def jsonEditPerson = Action.async(BodyParsers.parse.json) { request =>
    val restPerson = request.body.validate[RESTPerson]
    restPerson.fold(
      errors => {
        Future(BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toJson(errors))))
      },
      niceObject => {
        RESTPerson.doSomething(niceObject)
        val result = jsonSave(niceObject)
        result.map { booleanvalue =>
          println(booleanvalue)
          if (booleanvalue) {
            println("returning ok to sender")
            Ok(Json.obj("status" -> "OK", "message" -> ("Person '" + niceObject.name + "' saved.")))
          }
          else {
            println("returning KO to sender")
            BadRequest(Json.obj("status" -> "KO", "message" -> "Could not handle your request. Please verify email exists."))
          }
        }
      }
    )
  }


  def jsonSave(rESTPerson: RESTPerson):Future[Boolean] ={
    // get person by email
    println("Debug: jsonSave start")
    repo.getByEmail(rESTPerson.email).flatMap {
      case Some(person) =>
        repo.updatePerson(person.id , rESTPerson.name, rESTPerson.email,0, "", rESTPerson.description, false).map { number =>
          if (number == 1) {
            println("DEBUG: succeed to update " + number)
            Redirect(routes.PersonController.index())
            true
          }
          else
          {
            println("DEBUG: fail to update " + number)
            true
          }
        }
      case None => Future(false)
    }
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
    officesRepo.listMap().flatMap( offices =>
      repo.get(iId).map {
        case Some(person) =>
          val p = CreatePersonForm(person.name, person.email, person.location.id.toString,person.description, false )
          Ok(views.html.editPerson(personForm.fill(p) , offices,iId))
        case None => NotFound("Person Not Found")
      }
    )
  }

  def deletePerson(id: Long) = Action.async {
    repo.deleteRow(id).map {number =>
      if(number > 0) {
        println("DEBUG: reload index after delete --1--")
        Redirect(routes.PersonController.index())
      }
      else {
        println("DEBUG: fail row does not exist")
        Redirect(routes.PersonController.index())
      }
    }
  }

  def save (iId: Long) = Action.async(parse.multipartFormData) { implicit request =>
    //    println("Debug: start save")
    personForm.bindFromRequest.fold(
      errorForm => {
//                println("Debug: Saving edited person")
        officesRepo.listMap().flatMap( offices =>
          repo.listPersons().map(people =>
            Ok(views.html.editPerson(errorForm, offices, iId: Long))))
      },
      person => {
        repo.checkEmails(person.email, iId).flatMap {
          case Some(existingEmail) => {
            //            println("DEBUG: from edit:  email already exists")
            Future.successful(NotFound("from edit:  email already exists"))
          }
          case None => {
            //            println("DEBUG: from edit: New person. setimage = " + person.setphoto)
            val temp = if (person.setphoto)
              uploadFile(request, person.setphoto)
            else
              play.Play.application.configuration.getString("default_photo")
            //            println("Debug -- before update person" + person.name+ person.email+ person.location  )
            repo.updatePerson(iId: Long , person.name, person.email, person.location.toLong , temp, person.description, person.setphoto).map { number =>
              if (number == 1) {
                //                println("DEBUG: succeed to update " + number)
                Redirect(routes.PersonController.index())
              }
              else
              {
                println("DEBUG: faile to update " + number)
                Redirect(routes.PersonController.index())
              }
            }
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
    println("DEBUG: start addPerson")
    // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle succes.
    personForm.bindFromRequest.fold(
      // The error function. We return the index page with the error form, which will render the errors.
      // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
      // a future because the person creation function returns a future.
      errorForm => {
        officesRepo.listMap().flatMap(offices =>
          repo.listPersons().map(people =>
            Ok(views.html.index(errorForm, people, offices))))
      },
      // There were no errors in the from, so create the person.
      // at this point we have successful person object from html strings name and age
      person => {
        // check if person already exists
        repo.checkEmails(person.email).flatMap {
          case Some(existingEmail) => {
            //            println("DEBUG: email already exists")
            Future.successful(NotFound("email already exists"))
          }
          case None => {
            //            println("DEBUG: New person")
            val temp = uploadFile(request)
            repo.create_person(person.name, person.email, person.location , temp, person.description).map { person =>
              println(person.toString)
              // If successful, we simply redirect to the index page.
              Redirect(routes.PersonController.index())
            }
          }
        }
      }
    )
  }

  def uploadFile(request: Request[MultipartFormData[Files.TemporaryFile]], iSetPhoto: Boolean = true): String = {
    //    println("Debug: Request is: " +request.toString());
    request.body.file("photo").map { picture =>
      if(!picture.filename.isEmpty) {
        val filenameToUpload = play.Play.application.configuration.getString("pictures_path") + picture.filename
        val file = new File(filenameToUpload)
        picture.ref.moveTo(file)
        picture.filename
      }
      else play.Play.application.configuration.getString("default_photo")
    }.getOrElse(play.Play.application.configuration.getString("default_photo"))
  }

  /**
    * A REST endpoint that gets all the people as JSON.
    */
  def getPersons = Action.async {
    //    println("debug ---1---")
    repo.listPersons().map { people =>
      Ok(Json.toJson(people))
    }
  }
  // candidate for removal - not used
  def getPersons(email: String) = Action.async {
    //    println("debug ---1---")
    repo.listPersons().map { people =>
      Ok(Json.toJson(people))
    }
  }

  def getSinglePerson(email: String) = Action.async {
    //    println("debug ---1---")
    repo.checkEmails(email).map {
      case Some(person) => Ok(Json.toJson(person))
      case None => NotFound(Json.obj("Error"->"Person Not Found"))
    }
  }

  def giveMePicture(name: String) = Action {
    val myFile: File =
    if (!(name == play.Play.application.configuration.getString("default_photo") || name == ""))
      new File(play.Play.application.configuration.getString("pictures_path") + name)
    else
      new File("public/images/"+play.Play.application.configuration.getString("default_photo"))
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
case class CreatePersonForm(name: String, email: String, location: String, description: String, setphoto: Boolean)

//object CreatePersonForm {
//  //this is an example on how to have custom apply
//  def newApply ( name: String, email: String, description: String) = {
//    CreatePersonForm( name, email, selectLocation ,description, true)
//  }
//  def newUApply(ali: CreatePersonForm) = {
//    Some(ali.name, ali.email, ali.location,ali.description)
//  }
//
//  //  implicit val personRead :Reads[Person]= (
//  //      (JsPath \ "name").read[String] and
//  //      (JsPath \ "email").read[String] and
//  //      (JsPath \ "description").read[String]
//  //    )(Person.apply _)
//}
