package akkahttp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{FileIO, Flow, Framing, Source}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by sajith on 1/17/18.
  */
object Client {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/hello"))


    responseFuture
      .onComplete {
        case Success(response) => response.entity.withoutSizeLimit().dataBytes
          .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256))
          //          .throttle(1,0.01 millisecond,1,ThrottleMode.Shaping)
          .map((bs: ByteString) => bs.utf8String)
          .runForeach((str: String) => {
            Thread.sleep(10)
            println(str)
          })
        case Failure(x) => x.printStackTrace()
      }
  }

}
