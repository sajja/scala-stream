package logpublisher

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.{HttpCharsets, HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.duration.DurationInt
import akka.http.scaladsl.model.ContentTypes._

import scala.io.StdIn
import scala.collection.immutable
import scala.util.Random

/**
  * Created by sajith on 5/30/17.
  */
object ApiServer extends App with UserProtocol {
  implicit val system = ActorSystem("api")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher


  def loadUsers(): Source[User, NotUsed] = {
    Source {
      new immutable.Iterable[User] {
        override def iterator: Iterator[User] = new Iterator[User] {
          val ran = Random

          override def hasNext: Boolean = true

          override def next(): User = {
            val i = Random.nextInt
            val user = User(s"user$i", s"$i")
            println(user)
            user
          }
        }
      }
    }
  }

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
    .withParallelMarshalling(parallelism = 10, unordered = false)

  // (fake) async database query api
  def dummyUser(id: String) = User(s"User $id", id.toString)

  def fetchUsers(): Source[User, NotUsed] = loadUsers

  val route =
    pathPrefix("users") {
      get {
        complete(HttpResponse(entity=HttpEntity.Chunked(MediaTypes.`text/plain` withCharset HttpCharsets.`UTF-8`,
          fetchUsers().map(i=>ByteString(i.toString+"\n")))))
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ ⇒ system.terminate())
}
