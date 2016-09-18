package controllers

import java.io.File
import java.math.BigInteger
import java.net.URL
import java.security.SecureRandom
import javax.inject._

import models._
import services.{PersonRepository}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.Files
import play.api.libs.json.{JsError, Json}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

import com.lunatech.openconnect.Authenticate






class PersonController @Inject()(repo: PersonRepository, val messagesApi: MessagesApi, configuration: play.api.Configuration)
                                (implicit ec: ExecutionContext) extends Controller with I18nSupport with Secured  {

  /**
    *
    * The mapping for the person form.
    */
  val personForm: Form[Person] = Form {
    mapping(
      "id" -> optional(longNumber),
      "name" -> nonEmptyText,
      "email" -> email,
      "phone" -> optional(text),
      "location" -> text,
      "photo" -> optional(text),
      "description" -> text
    )(Person.apply)(Person.unapply)
  }

  def index() = IsAuthenticated { username => implicit request =>
    search(repo.all())
  }

  /**
    * The index action.
    */
  def searchByLocation(office: String) = IsAuthenticated { username => implicit request =>

    search(repo.findByLocation(office), office)
  }

  private def search(p: Future[Seq[Person]], office: String = "all") = {
    for {
      people <- p
      countAll <- repo.count()
      countRotterdam <- repo.countByLocation("rotterdam")
      countMontevrain <- repo.countByLocation("montevrain")
    } yield {
        Ok(views.html.index(personForm, people, countAll, countRotterdam, countMontevrain, office))
    }
  }

  def delete(id: Long) = IsAuthenticated { username => implicit request =>
    repo.delete(id).map { number =>
        Redirect(routes.PersonController.index())
      }

  }

  def edit(id: Long) = IsAuthenticated { username => implicit request =>
    for {
      people <- repo.all()
      person <- repo.findById(id)
      countAll <- repo.count()
      countRotterdam <- repo.countByLocation("rotterdam")
      countMontevrain <- repo.countByLocation("montevrain")
    } yield {
        Ok(views.html.index(personForm.fill(person.get), people, countAll, countRotterdam, countMontevrain))
    }

  }

  def update(id: Long) = IsAuthenticated { username => implicit request =>
    personForm.bindFromRequest.fold(
      errorForm => {
        for {
          people <- repo.all()
          countAll <- repo.count()
          countRotterdam <- repo.countByLocation("rotterdam")
          countMontevrain <- repo.countByLocation("montevrain")
        } yield {
            BadRequest(views.html.index(errorForm, people, countAll, countRotterdam, countMontevrain))
        }
      },
      person => {
          for (p <- repo.update(person)) yield
            Redirect(routes.PersonController.index())
      }
    )
  }
  /**
    * The add person action.
    *
    * This is asynchronous, since we're invoking the asynchronous methods on PersonRepository.
    */
  def create() = IsAuthenticated { username => implicit request =>
    // This will trigger validation
    personForm.bindFromRequest.fold(
      errorForm => {
        for {
          people <- repo.all()
          countAll <- repo.count()
          countRotterdam <- repo.countByLocation("rotterdam")
          countMontevrain <- repo.countByLocation("montevrain")
        } yield {
            BadRequest(views.html.index(errorForm, people, countAll, countRotterdam, countMontevrain))
        }
      },
      person => {
          // Create our person
          for (p <- repo.insert(person)) yield
            Redirect(routes.PersonController.index())
      }
    )
  }

  def saveImage(personId: Long) = Action.async(parse.multipartFormData) { implicit request =>
    val uploadedFile = request.body.files.head.ref
    val id = new BigInteger(130, new SecureRandom()).toString(32)
    // Update our person with new Id
    for {
        updatedPerson <- repo.updatePhoto(personId, id)
    } yield {
      // Save our file to the disk for now. Latter on it has to be on the clevercloud S3
      val file = new File(configuration.getString("pictures.path").getOrElse("/tmp/") + id)
      uploadedFile.moveTo(file)
      Ok(id)
    }
  }

  def getImage(id: String) = IsAuthenticated { username => implicit request =>
    val file = new File(configuration.getString("pictures.path").getOrElse("/tmp/") + id)
    Future.successful(Ok.sendFile(
      content= file,
      fileName = _ => id + ".png"))
  }

  /**
    * A REST endpoint that gets all the people as JSON.
    */
  def all() = Action.async {
    repo.all().map { people =>
      Ok(Json.toJson(people))
    }
  }

  /**
    * Login page.
    */
  def login = Action { implicit request =>
    if (play.Play.isProd) {
      val clientId: String = play.Play.application.configuration.getString("google.clientId")
      val state: String = new BigInteger(130, new SecureRandom()).toString(32)
      Ok(views.html.login(clientId)).withSession("state" -> state)
    } else {
      Redirect(routes.PersonController.index).withSession("email" -> "developer@lunatech.com")
    }
  }

  def authenticate(code: String, id_token: String, access_token: String) = Action.async { implicit request => {
      val response = Authenticate.authenticateToken(code, id_token, access_token)
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
  private def username(request: RequestHeader) = {
      request.session.get("email")
  }
  /**
    * Redirect to login if the user in not authorized.
    */
  private def onUnauthorized(request: RequestHeader) = {
    Results.Redirect(routes.PersonController.login).withSession("originalUrl" -> request.uri)
  }

  // --

  /**
    * Action for authenticated users.
    */
  def IsAuthenticated(f: => String => Request[AnyContent] => Future[Result]) =
  {
    Security.Authenticated(username, onUnauthorized) { user => {
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

 def IsAdmin(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
   Future.successful {
     if (isAdmin(user)) {
       f(user)(request)
     } else {
       Results.Forbidden("you are not admin")
     }
   }
 }

 def isAdmin(user: String) = play.Play.application.configuration.getString("administrator").contains(user)

}
