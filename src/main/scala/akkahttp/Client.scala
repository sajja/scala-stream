package akkahttp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{FileIO, Flow, Framing, Source}
import akka.util.ByteString
import akkahttp.WebServer.MyJsonProtocol.jsonFormat2

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by sajith on 1/17/18.
  */
object Client {

  case class Tweet(uid: Int, txt: String)

  case class Measurement(id: String, value: Int)

  object MyJsonProtocol
    extends akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
      with spray.json.DefaultJsonProtocol {

    implicit val tweetFormat = jsonFormat2(Tweet.apply)
    implicit val measurementFormat = jsonFormat2(Measurement.apply)
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/tweets"))


    responseFuture
      .onComplete {
        case Success(response) => response.entity.withoutSizeLimit().dataBytes
          //          .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 560))
          .runForeach(i => {
          println(i)
          Thread.sleep(1)
        })
        case Failure(x) => x.printStackTrace()
      }
  }

}


object XX {

  abstract class FT[T] {
    def isEnable(t: T): Unit
  }

  abstract class Show[A] {
    def show(a: A): String
  }

  class idX extends FT[Int] {
    override def isEnable(t: Int): Unit = ???
  }

  class IdFT extends FT[Array[Int]] {
    override def isEnable(t: Array[Int]): Unit = ???
  }

  sealed abstract class FTE[T](ft:FT[T])
  case object DC_FT extends FTE[Int](new idX())

  def main(args: Array[String]): Unit = {


  }
}