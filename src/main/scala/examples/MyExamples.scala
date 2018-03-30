package examples

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream._
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Keep, Merge, Partition, RunnableGraph, Sink, Source}
import akka.util.ByteString
import org.reactivestreams.{Subscriber, Subscription}

import scala.collection
import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

class Event

case class Impression(adName: String) extends Event

case class Click(adName: String) extends Event


object MyExamples {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  case class Event(et: String, ad: String, value: Int)


  def t1() = {
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val in = Source(1 to 10)
      val out = Sink.foreach(println)

      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

      in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
      bcast ~> f4 ~> merge
      ClosedShape
    }).run()
  }

  case class Doc(id: Int, endpoint: String)

  def generateDoc() = {
    val allEndPoints = List("21G", "BASWARE", "1stBP", "HansaPrint")
    Doc(Random.nextInt(1000), allEndPoints(Random.nextInt(3)))
  }

  def delivery2(): Unit = {
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val input = Source.fromIterator(() => collection.Iterator.continually[Doc] {
        Thread.sleep(100)
        generateDoc()
      })

      val batcher = Flow[Doc].map(x => x).grouped(12)
      val sink = Sink.foreach(println)

      input ~> batcher ~> sink

      ClosedShape
    }).run()
  }

  def delivery(): Unit = {
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val input = Source.fromIterator(() => collection.Iterator.continually[Doc] {
        Thread.sleep(100)
        generateDoc()
      })
      val twg = Sink.foreach[Doc](x => println(s"TWG stream $x)"))
      val bw = Sink.foreach[Doc](x => println(s"BW steam $x"))
      val bp = Sink.foreach[Doc](x => println(s"BP steam $x"))
      val hp = Sink.foreach[Doc](x => println(s"HP steam $x"))
      val other = Sink.foreach[Doc](x => println(s"Other steam $x"))

      val batcher = Flow[Doc].map(x => x).grouped(30)

      val endpoint = builder.add(Partition[Doc](5, doc => doc.endpoint match {
        case "21G" =>
          0
        case "BASWARE" =>
          1
        case "1stBP" =>
          2
        case "HansaPrint" =>
          3
        case _ =>
          4
      }))

      input ~> endpoint.in
      val sink = Sink.foreach(println)

      endpoint.out(0) ~> batcher ~> sink
      endpoint.out(1) ~> bw
      endpoint.out(2) ~> bp
      endpoint.out(3) ~> hp
      endpoint.out(4) ~> other

      ClosedShape
    }).run()
  }

  def t2() = {
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val in1 = Source(1 to 10).throttle(1, 1 seconds, 1, ThrottleMode.shaping)
      val in2 = Source(10 to 20)
      val out = Sink.foreach(println)

      //      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      val f1, f2 = Flow[Int].map((i: Int) => i)
      //      in ~> bcast ~>f1 ~>out
      //      bcast ~>f2 ~> out
      in1 ~> merge ~> out
      in2 ~> merge

      ClosedShape
    }).run()
  }

  def t3() = {
    val txStream = ccTransactions().throttle(1, 100 millisecond, 1, ThrottleMode.Shaping)
    val fraudStream = txStream.filter(tx => tx._2 > 400)
    fraudStream.runForeach(println)
  }

  /**
    * Requirement
    * -- Ad with more than 3 imp. for a sec is suspicious
    * -- Ad with more than 3 clicks for a sec is suspicious
    *
    * -- Flag any ad with more than 3 clicks and impressions
    */
  def t4() = {
    val is: Source[Iterable[Event], NotUsed] = impressions()
      .throttle(1, 1 microsecond, 1, ThrottleMode.Shaping) ///just to slow down
      .groupedWithin(10, 1 seconds) //takes a micro batch every second
      .map(imps => imps.map(imp => (imp.adName, 1)).groupBy(_._1).mapValues(_.size) //reduce the batch and combine result
      .filter(imp => imp._2 > 3) //Take only suspicious ads
      .map(imp => Event("IMPRESSION", imp._1, imp._2)) //Transform to a common format
    )

    val cs = clicks()
      .throttle(1, 1 millisecond, 1, ThrottleMode.Shaping) //just to slow down
      .groupedWithin(10, 1 seconds) //take a micro batch each second
      .map(imps => imps.map(imp => (imp.adName, 1)).groupBy(_._1).mapValues(_.size) //reduce batch combine result
      .filter(click => click._2 > 3) //Take only suspicious ads
      .map(click => Event("CLICK", click._1, click._2)) //Transform to common format
    )

    //    is.runForeach(println)
    //    cs.runForeach(println)
    def filterEvent(events: Seq[Iterable[Event]], eventType: String): Set[String] = {
      (for {
        event <- events.flatten if event.et == eventType
      } yield event.ad).toSet
    }

    def getImps(events: Seq[Iterable[Event]]) = filterEvent(events, "IMPRESSION")

    def getClicks(events: Seq[Iterable[Event]]) = filterEvent(events, "CLICK")

    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val out = Sink.foreach[Iterable[String]](l =>
        //        if (l.nonEmpty) {
        println(l)
        //        }
      )

      val merge = builder.add(Merge[Iterable[Event]](2))

      val f = Flow[Iterable[MyExamples.Event]].map(e => e)
        .groupedWithin(10, 1 seconds)
        .map((events: immutable.Seq[Iterable[Event]]) => {
          val impressions = getImps(events)
          val clicks = getClicks(events)
          val fraude = impressions.intersect(clicks)
          fraude
        })
      is ~> merge
      cs ~> merge
      merge ~> f ~> out
      ClosedShape
    }).run()

  }

  def main(args: Array[String]): Unit = {
    delivery()
  }

  def ccTransactions() = Source.fromIterator(() => Iterator.continually((Random.nextInt(10), Random.nextInt(500))))

  def impressions() = Source.fromIterator(() => Iterator.continually(Random.nextInt(5)).map(i => Impression(s"ad$i")))

  def clicks(): Source[Click, NotUsed] = Source.fromIterator(() => Iterator.continually(Random.nextInt(5)).map(i => Click(s"ad$i")))

  //  def words() = Source(("xx",1)::("gg",1)::("xx",1)::Nil)
  def words() = Source(1 to 1000).map((i: Int) => (s"$i", 1))

  def words1() = Source.fromIterator(() => Iterator.continually({
    "XX"
  }))

}

