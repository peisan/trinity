package org.sisioh.trinity.example

import com.twitter.finagle.http.Response
import com.twitter.util.{FuturePool, Future}
import java.util.concurrent.Executors
import org.jboss.netty.handler.codec.http.HttpMethod
import org.sisioh.trinity.application.TrinityApplication
import org.sisioh.trinity.domain._
import org.sisioh.trinity.view.ScalateView
import scala.concurrent._

/**
 * Custom Error Handling with custom Exception
 *
 * curl http://localhost:7070/unautorized
 */
class UnauthorizedException extends Exception

object TrinityExample {

  val threadPool = Executors.newCachedThreadPool()
  implicit val futurePool = FuturePool(threadPool)
  implicit val executor = ExecutionContext.fromExecutor(threadPool)

  class PlayLikeController(application: TrinityApplication) extends AbstractController(application) {

    def index = SimpleAction {
      request: Request =>
        responseBuilder.withOk.build
    }

    def getUser = FutureAction {
      request: Request =>
        val name = request.routeParams("name")
        responseBuilder.withBody(name).toFuture
    }

    def getGroup = ScalaFutureAction {
      request: Request =>
        future {
          val name = request.routeParams("name")
          responseBuilder.withBody(name).build
        }
    }

    addRoute(HttpMethod.GET, "/", index)
    addRoute(HttpMethod.GET, "/user/:name", getUser)
    addRoute(HttpMethod.GET, "/group/:name", getGroup)

  }


  class ExampleController(application: TrinityApplication) extends ScalatraLikeController(application) {

    /**
     * Basic Example
     *
     * curl http://localhost:7070/hello => "hello world"
     */
    get("/") {
      request =>
        responseBuilder.withPlain("hello world").toFuture
    }

    /**
     * Route parameters
     *
     * curl http://localhost:7070/user/dave => "hello dave"
     */
    get("/user/:username") {
      request =>
        val username = request.routeParams.getOrElse("username", "default_user")
        responseBuilder.withPlain("hello " + username).toFuture
    }

    /**
     * Setting Headers
     *
     * curl -I http://localhost:7070/headers => "Foo:Bar"
     */
    get("/headers") {
      request =>
        responseBuilder.withPlain("look at headers").withHeader("Foo", "Bar").toFuture
    }

    /**
     * Rendering json
     *
     * curl -I http://localhost:7070/headers => "Foo:Bar"
     */
    get("/data.json") {
      request =>
        import org.json4s.JsonDSL._
        responseBuilder.withJson(Map("foo" -> "bar")).toFuture
    }

    /**
     * Query params
     *
     * curl http://localhost:7070/search?q=foo => "no results for foo"
     */
    get("/search") {
      request =>
        request.params.get("q") match {
          case Some(q) => responseBuilder.withPlain("no results for " + q).toFuture
          case None => responseBuilder.withPlain("query param q needed").withStatus(500).toFuture
        }
    }

    /**
     * Uploading files
     *
     * curl -F avatar=@/path/to/img http://localhost:7070/profile
     */
    post("/profile") {
      request =>
        request.multiParams.get("avatar").map {
          avatar =>
            println("content type is " + avatar.contentType)
            avatar.writeToFile("/tmp/avatar") //writes uploaded avatar to /tmp/avatar
        }
        responseBuilder.withPlain("ok").toFuture
    }

    get("/template") {
      request =>
        responseBuilder.withBody(ScalateView("template.mustache", Map("some_val" -> "random value here"))).toFuture
    }


    /**
     * Custom Error Handling
     *
     * curl http://localhost:7070/error
     */
    get("/error") {
      request =>
        1234 / 0
        responseBuilder.withPlain("we never make it here").toFuture
    }


    get("/unauthorized") {
      request =>
        throw new UnauthorizedException
    }


    /* Dispatch based on Content-Type
    *
    * curl http://localhost:7070/index.json
    * curl http://localhost:7070/index.html
    */
    get("/blog/index.:format") {
      request =>
        import org.json4s.JsonDSL._
        respondTo(request) {
          case ContentType.TextHtml => responseBuilder.withHtml("<h1>Hello</h1>").toFuture
          case ContentType.AppJson => responseBuilder.withJson(Map("value" -> "hello")).toFuture
        }
    }

    /**
     * Also works without :format route using browser Accept header
     *
     * curl -H "Accept: text/html" http://localhost:7070/another/page
     * curl -H "Accept: application/json" http://localhost:7070/another/page
     * curl -H "Accept: foo/bar" http://localhost:7070/another/page
     */

    get("/another/page") {
      request =>
        respondTo(request) {
          case ContentType.TextHtml => responseBuilder.withPlain("an html response").toFuture
          case ContentType.AppJson => responseBuilder.withPlain("an json response").toFuture
          case ContentType.All => responseBuilder.withPlain("default fallback response").toFuture
        }
    }

    /**
     * Metrics are supported out of the box via Twitter's Ostrich library.
     * More details here: https://github.com/twitter/ostrich
     *
     * curl http://localhost:7070/slow_thing
     *
     * By default a stats server is started on 9990:
     *
     * curl http://localhost:9990/stats.txt
     *
     */

    get("/slow_thing") {
      request =>
        stats.counter("slow_thing").incr()
        stats.time("slow_thing time") {
          Thread.sleep(100)
        }
        responseBuilder.withPlain("slow").toFuture
    }
  }


  def main(args: Array[String]) = {

    val globalSettings = new GlobalSetting {
      def error(request: Request): Future[Response] = {
        request.error match {
          case Some(e: ArithmeticException) =>
            ResponseBuilder().withStatus(500).withPlain("whoops, divide by zero!").toFuture
          case Some(e: UnauthorizedException) =>
            ResponseBuilder().withStatus(401).withPlain("Not Authorized!").toFuture
          case Some(e) =>
            ResponseBuilder().withStatus(415).withPlain("Unsupported Media Type!").toFuture
          case _ =>
            ResponseBuilder().withStatus(500).withPlain("Something went wrong!").toFuture
        }
      }

      def notFound(request: Request): Future[Response] = {
        ResponseBuilder().withStatus(404).withPlain("not found yo").toFuture
      }

    }

    val config = Config()
    val application = TrinityApplication(config, Some(globalSettings))
    application.registerController(new PlayLikeController(application))
    application.registerController(new ExampleController(application))
    application.start()

  }
}