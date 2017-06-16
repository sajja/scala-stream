package logpublisher

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.io.StdIn

/**
  * Created by sajith on 5/30/17.
  */
object ApiServer extends App with UserProtocol {
  implicit val system = ActorSystem("api")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher


  def loadUsers: Stream[User] = Stream.cons(UserFactory.createUser(), {
    Thread.sleep(10)
    loadUsers
  })

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
    .withParallelMarshalling(parallelism = 10, unordered = false)

  // (fake) async database query api
  def dummyUser(id: String) = User(s"User $id", id.toString)

  def fetchUsers(): Source[User, NotUsed] = Source(loadUsers)

  val route =
    pathPrefix("users") {
      get {
        complete(fetchUsers())
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ ⇒ system.terminate())
}
