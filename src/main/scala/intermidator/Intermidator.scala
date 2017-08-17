package intermidator

import scala.io.StdIn
import scala.util.{Try, Success, Failure}
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.util.ByteString
import scala.concurrent.ExecutionContext.Implicits.global

object Intermidator {

  def main(args: Array[String]) {
    implicit val system = ActorSystem("QuickStart")
    implicit val materializer = ActorMaterializer()


    val route =
      get {
        val req = HttpRequest(uri = "http://localhost:8080/users")
        val reqFuture = Http().singleRequest(req)

        val logLinesStreamFuture: Future[Source[String, Any]] = reqFuture.map {
          response => response.entity.dataBytes.map(_.utf8String)
        }
        onSuccess(logLinesStreamFuture) { stream ⇒
          complete {
            HttpResponse(
              entity = HttpEntity.Chunked(MediaTypes.`text/plain` withCharset HttpCharsets.`UTF-8`,
                stream.map(line ⇒ ByteString(line + '\n', "UTF8"))))
          }
        }
      }


    val binding = Http().bindAndHandle(route, "localhost", 8081)
    binding.onComplete {
      case Success(x) =>
        println(s"Server is listening on ${x.localAddress.getHostName}:${x.localAddress.getPort}")
      case Failure(e) =>
        println(s"Binding failed with ${e.getMessage}")
    }
  }
}
