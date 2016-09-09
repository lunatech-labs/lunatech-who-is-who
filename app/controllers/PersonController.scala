package controllers

import java.io.File
import java.math.BigInteger
import java.net.URL
import java.security.SecureRandom
import javax.inject._

import dal.{OfficeRepository, PersonRepository}
import models.RESTPerson
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.Files
import play.api.libs.json.{JsError, Json}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

import com.lunatech.openconnect.Authenticate






class PersonController @Inject()(repo: PersonRepository, officesRepo: OfficeRepository, val messagesApi: MessagesApi, configuration: play.api.Configuration)
                                (implicit ec: ExecutionContext) extends Controller with I18nSupport with Secured  {

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
//  def index = Action.async {
  def index = IsAuthenticated { username => implicit request =>
//    Logger.info("avoid deprecated method: "+ play.Play.application.configuration.getString("default_photo"))
//    Logger.info("avoid deprecated method: "+ configuration.underlying.getString("default_photo"))
    {Logger.debug("Logger.debug - Debug: calling list map")
    officesRepo.listMap().flatMap( offices => {
      Logger.debug("Debug: ----d20--")
      repo.listPersons_merge.map(people =>{
        Logger.debug("Debug: ----d22--")
        for (c <- people)
          Logger.debug (c.toString)
        Ok(views.html.index(personForm, people, offices ))})})}
  }
//  def abc = IsAuthenticated { username => implicit request =>
//    println("I am calling secured")
//    Ok("HAPPY")
//  }
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
          Logger.debug(booleanvalue.toString)
          if (booleanvalue) {
            Logger.debug("returning ok to sender")
            Ok(Json.obj("status" -> "OK", "message" -> ("Person '" + niceObject.name + "' saved.")))
          }
          else {
            Logger.debug("returning KO to sender")
            BadRequest(Json.obj("status" -> "KO", "message" -> "Could not handle your request. Please verify email exists."))
          }
        }
      }
    )
  }


  def jsonSave(rESTPerson: RESTPerson):Future[Boolean] ={
    // get person by email
    Logger.debug("Debug: jsonSave start")
    repo.getByEmail(rESTPerson.email).flatMap {
      case Some(person) =>
        repo.updatePerson(person.id , rESTPerson.name, rESTPerson.email,person.location.id, "", rESTPerson.description, false).map { number =>
          if (number == 1) {
            Logger.debug("DEBUG: succeed to update " + number)
            Redirect(routes.PersonController.index())
            true
          }
          else
          {
            Logger.debug("DEBUG: fail to update " + number)
            true
          }
        }
      case None => Future(false)
    }
  }

//  def getDetailedPerson(id: Long) = Action.async {
  def getDetailedPerson(id: Long) = IsAuthenticated { username => implicit request =>
    Logger.debug("Debug: getDetailedPerson start here ")
    repo.get(id).map {
      case Some(person) => Ok(views.html.detailedPerson(person))
      case None => NotFound("Person Not Found")
    }
  }
  def editPerson(iId: Long) = Action.async { implicit request =>
    Logger.debug("Debug: editPerson start")
    officesRepo.listMap().flatMap( offices =>
      repo.get(iId).map {
        case Some(person) =>
          val p = CreatePersonForm(person.name, person.email, person.location.id.toString,person.description, false )
          Ok(views.html.editPerson(personForm.fill(p) , offices,iId))
        case None => NotFound("Person Not Found")
      }
    )
  }

  def deletePerson(id: Long) = IsAuthenticated { username => implicit request =>
    repo.deleteRow(id).map {number =>
      if(number > 0) {
        Logger.debug("DEBUG: reload index after delete --1--")
        Redirect(routes.PersonController.index())
      }
      else {
        Logger.debug("DEBUG: fail row does not exist")
        Redirect(routes.PersonController.index())
      }
    }
  }

  def save (iId: Long) = Action.async(parse.multipartFormData) { implicit request =>
    Logger.debug("Debug: start save")
    personForm.bindFromRequest.fold(
      errorForm => {
//                Logger.debug("Debug: Saving edited person")
        officesRepo.listMap().flatMap( offices =>
          repo.listPersons().map(people =>
            Ok(views.html.editPerson(errorForm, offices, iId: Long))))
      },
      person => {
        Logger.debug(person.toString)
        repo.checkEmails(person.email, iId).flatMap {
          case Some(existingEmail) => {
            //            Logger.debug("DEBUG: from edit:  email already exists")
            Future.successful(NotFound("from edit:  email already exists"))
          }
          case None => {
            //            Logger.debug("DEBUG: from edit: New person. setimage = " + person.setphoto)
            val temp = if (person.setphoto)
              uploadFile(request, person.setphoto)
            else
              configuration.underlying.getString("default_photo")
            //            Logger.debug("Debug -- before update person" + person.name+ person.email+ person.location  )
            repo.updatePerson(iId: Long , person.name, person.email, person.location.toLong , temp, person.description, person.setphoto).map { number =>
              if (number == 1) {
                //                Logger.debug("DEBUG: succeed to update " + number)
                Redirect(routes.PersonController.index())
              }
              else
              {
                Logger.debug("DEBUG: faile to update " + number)
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
    Logger.debug("DEBUG: start addPerson")
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
        Logger.debug ( "DEBUG " + person.toString )
        if (person.location == None)
          Logger.debug ("nulll")
        else if (person.location.isEmpty)
          Logger.debug ("empty")
        repo.checkEmails(person.email).flatMap {
          case Some(existingEmail) => {
            //            Logger.debug("DEBUG: email already exists")
            Future.successful(NotFound("email already exists"))
          }
          case None => {
            //            Logger.debug("DEBUG: New person")
            val temp = uploadFile(request)
            repo.create_person(person.name, person.email, person.location , temp, person.description).map { person =>
              Logger.debug(person.toString)
              // If successful, we simply redirect to the index page.
              Redirect(routes.PersonController.index())
            }
          }
        }
      }
    )
  }

  def uploadFile(request: Request[MultipartFormData[Files.TemporaryFile]], iSetPhoto: Boolean = true): String = {
    //    Logger.debug("Debug: Request is: " +request.toString());
    request.body.file("photo").map { picture =>
      if(!picture.filename.isEmpty) {
        val filenameToUpload = configuration.underlying.getString("pictures_path") + picture.filename
        val file = new File(filenameToUpload)
        picture.ref.moveTo(file)
        picture.filename
      }
      else configuration.underlying.getString("default_photo")
    }.getOrElse(configuration.underlying.getString("default_photo"))
  }

  /**
    * A REST endpoint that gets all the people as JSON.
    */
  def getPersons = Action.async {
    //    Logger.debug("debug ---1---")
    repo.listPersons().map { people =>
      Ok(Json.toJson(people))
    }
  }
  // candidate for removal - not used
  def getPersons(email: String) = Action.async {
    //    Logger.debug("debug ---1---")
    repo.listPersons().map { people =>
      Ok(Json.toJson(people))
    }
  }

  def getSinglePerson(email: String) = Action.async {
    //    Logger.debug("debug ---1---")
    repo.checkEmails(email).map {
      case Some(person) => Ok(Json.toJson(person))
      case None => NotFound(Json.obj("Error"->"Person Not Found"))
    }
  }

  def giveMePicture(name: String) = Action {
    val myFile: File =
    if (!(name == configuration.underlying.getString("default_photo") || name == ""))
      new File(configuration.underlying.getString("pictures_path") + name)
    else
      new File("public/images/"+configuration.underlying.getString("default_photo"))
    Ok.sendFile(myFile)
  }



  /**
    * Login page.
    */
  def login = Action { implicit request =>
//    if(play.Play.isProd) {
    println ("Debug login ----0-----")
    if(play.Play.isProd || play.Play.isDev) {
      println ("Debug login ----1-----")
//      val clientId: String = Play.configuration.getString("google.clientId").get
      val clientId: String = play.Play.application.configuration.getString("google.clientId")
      val state: String = new BigInteger(130, new SecureRandom()).toString(32)
      println ("Debug login ----1-----"+clientId)
      Ok(views.html.login(clientId)).withSession("state" -> state)
    } else {
      println ("Debug login ----2-----")
      Redirect(routes.PersonController.index).withSession("email" -> "developer@laliunatech.com")
    }
  }

  def authenticate(code: String, id_token: String, access_token: String) = Action.async { implicit request => {
    println ("Debug authenticate ----0-----"+code+" "+id_token+" "+access_token)
    val response = Authenticate.authenticateToken(code, id_token, access_token)
    println ("Debug authenticate ----1-----")
    response.map {
      case Left(parameters) => Redirect(routes.PersonController.index).withSession(parameters.toArray: _*)
      case Right(message) => Redirect(routes.PersonController.login).withNewSession.flashing("error" -> message.toString())
    }
  }
  }




  /**
    * Logout and clean the session.
    */
  def logout = Action {
    Redirect(routes.PersonController.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }
}

/**
  * The create person form.
  *
  */
case class CreatePersonForm(name: String, email: String, location: String, description: String, setphoto: Boolean)



/**
  * Provide security features
  */
trait Secured {
  import com.google.api.client.auth.oauth2.TokenResponseException
  import com.google.api.client.googleapis.auth.oauth2.{GoogleCredential, GoogleAuthorizationCodeTokenRequest, GoogleTokenResponse}
  import com.google.api.client.http.javanet.NetHttpTransport
  import com.google.api.client.json.jackson2.JacksonFactory
  import com.google.api.services.oauth2.Oauth2
  import com.google.api.services.oauth2.model.Tokeninfo
  import com.google.gdata.client.authn.oauth.GoogleOAuthParameters
  import com.google.gdata.client.authn.oauth.OAuthRsaSha1Signer
  import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer
  import com.google.gdata.client.authn.oauth.OAuthParameters.OAuthType
  import com.google.gdata.client.appsforyourdomain.UserService
  import com.google.gdata.data.appsforyourdomain.provisioning.UserFeed

  /**
    * Retrieve the connected user email.
    */
//  private def username(request: RequestHeader) = request.session.get("email")
  private def username(request: RequestHeader) = {
//    println("DEBUG username --- 1 ----" )
    val us = request.session.get("email")
//  println("DEBUG username --- 1 ----" + us)
  us
  }
  /**
    * Redirect to login if the user in not authorized.
    */
  private def onUnauthorized(request: RequestHeader) = {
    println("DEBUG onUnauthorized --- 1 ----" )
    Results.Redirect(routes.PersonController.login).withSession("originalUrl" -> request.uri)
  }

  // --

  /**
    * Action for authenticated users.
    */
  def IsAuthenticated(f: => String => Request[AnyContent] => Future[Result]) =
  {
//    println("DEBUG IsAuthenticated --- 1 ----" )
    Security.Authenticated(username, onUnauthorized) { user =>{
//      println("DEBUG IsAuthenticated --- 2 ----" )
      Action.async(request => f(user)(request))}}
  }

  def isOnWhiteList(email:String) = {
    import play.api.Play.current
    val CONSUMER_KEY = play.Play.application.configuration.getString("google.key")
    val CONSUMER_SECRET =  play.Play.application.configuration.getString("google.secret")
    val DOMAIN =  play.Play.application.configuration.getString("google.domain")

    val oauthParameters = new GoogleOAuthParameters()
    oauthParameters.setOAuthConsumerKey(CONSUMER_KEY)//removed.get
    oauthParameters.setOAuthConsumerSecret(CONSUMER_SECRET)//removed.get
    oauthParameters.setOAuthType(OAuthType.TWO_LEGGED_OAUTH)
    val signer = new OAuthHmacSha1Signer()
    val feedUrl = new URL("https://apps-apis.google.com/a/feeds/" + DOMAIN + "/user/2.0")

    val service = new UserService("ProvisiongApiClient")
    service.setOAuthCredentials(oauthParameters, signer)
    service.useSsl()
    val resultFeed = service.getFeed(feedUrl,  classOf[UserFeed])

    import scala.collection.JavaConversions._
    val users =  resultFeed.getEntries.toSet
    val filteredUsers = users.map( entry => entry.getTitle().getPlainText() + "@" + DOMAIN)

    filteredUsers.contains(email)
  }

//  def IsAdmin(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
//    if (isAdmin(user)) {
//      f(user)(request)
//    } else {
//      Results.Forbidden("you are not admin")
//    }
//  }
//
//  def isAdmin(user: String) = play.Play.application.configuration.getString("administrator").get.contains(user)

}

