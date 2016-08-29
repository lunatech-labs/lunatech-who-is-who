import java.util.concurrent.TimeUnit

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.test._
import play.api.test.Helpers._
import play.api.test._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification {


  "Application" should {

    "work from within a browser" in new WithBrowser {
//      val port1 = 9000
      browser.goTo("http://localhost:" + port)
      browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded
      browser.pageSource must contain("Save Person")
    }
  }

  // these tests run only with memory data base H2
  // otherwise will get error resulting from duplicates....
  "Application" should {
    "work from within a browser" in new WithBrowser {
      browser.goTo("http://localhost:" + port)
      browser.fill("#name").`with`("test VALIDATE")
      browser.fill("#email").`with`("test.VALIDATE@gmail.com")
      browser.fill("#description").`with`("abc ...")
      browser.submit("#savePerson")
      browser.await().atMost(5, TimeUnit.SECONDS).untilPage().isLoaded
      browser.goTo("http://localhost:" + port)
//      println(browser.pageSource)
//      assert(browser.title().contains("Upload")) // FAILS HERE
      browser.pageSource must contain("abc ...")
    }
  }


}
