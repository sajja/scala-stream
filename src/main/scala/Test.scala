import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Request
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Random, Try}

class MyAtor extends Actor with ActorPublisher[Int] {
  var items: List[Int] = List.empty

  def receive = {
    case s: String =>
      println(totalDemand)
      Thread.sleep(500)
      println(items)
      if (totalDemand == 0)
        items = items :+ s.toInt
      else
        onNext(s.toInt)

    case Request(demand) =>
      if (demand > items.size) {
        println("VVVVVVVV")
        items foreach (onNext)
        items = List.empty
      }
      else {
        println("VVVVVVVV")
        val (send, keep) = items.splitAt(demand.toInt)
        items = keep
        send foreach (onNext)
      }


    case other =>
      println(s"got other $other")
  }
}

object Test {
  def test1(): Unit = {
    implicit val system = ActorSystem("QuickStart")
    implicit val materializer = ActorMaterializer()
    val source = Source(1 to 10)
    val sink = Sink.fold[Int, Int](0)(_ + _)
    val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)
    val sum: Future[Int] = runnable.run()
    sum.onComplete((triedInt: Try[Int]) => println(triedInt.get))
  }

  def test2(): Unit = {
    implicit val system = ActorSystem("QuickStart")
    implicit val materializer = ActorMaterializer()
    val source = Source(1 to 10)
    val sink = Sink.fold[Int, Int](0)(_ + _)
    val sum: Future[Int] = source.runWith(sink)
    sum.onComplete((triedInt: Try[Int]) => println(triedInt.get))
  }

  def test3(): Unit = {
    implicit val system = ActorSystem("QuickStart")
    implicit val materializer = ActorMaterializer()
    val source = Source(List(1, 2, 3))
    val sink = Sink.head[Int]
    val sum = source.runWith(sink)
    sum.onComplete(println)
  }

  def test4(): Unit = {
    implicit val system = ActorSystem("QuickStart")
    implicit val materializer = ActorMaterializer()

    val actorRef = system.actorOf(Props[MyAtor])
    val pub = ActorPublisher[Int](actorRef)
    for (i <- 1 until 20) {
      actorRef ! i.toString
    }
    println("XX")
    val source = Source.fromPublisher(pub)
    source.take(100).runForeach {
      s =>
//        Thread.sleep(1000)
        println("===> " + s)
    }
  }


  def main(args: Array[String]): Unit = {
    //    test1()
    //    test2()
    //    test3()
    test4()
  }
}
