package intermidator

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}
object Step0 extends App {
  //https://github.com/jrudolph/scala-world-2015/blob/master/backend/src/main/scala/example/repoanalyzer/Step0.scala
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val logStreamRequest = HttpRequest(uri = "http://localhost:8080/users")
  val logStreamResponseFuture = Http().singleRequest(logStreamRequest) // Future[HttpResponse]
  val logLinesStreamFuture: Future[Source[String, Any]] =
    logStreamResponseFuture.map { response ⇒
      response.entity.dataBytes
        .map(_.utf8String)
    }

  logLinesStreamFuture.onComplete {
    case Success(logStream) ⇒ logStream.runForeach(println)
    case Failure(e) ⇒
      println("Request failed")
      e.printStackTrace()
      system.terminate()
  }

  StdIn.readLine()
  system.terminate()
}
