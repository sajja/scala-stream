package actor_stream

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, IOResult, OverflowStrategy, ThrottleMode}
import akka.util.ByteString
import org.reactivestreams.{Subscriber, Subscription}

import scala.collection.immutable
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Random

case class Data(sender: Option[String], body: String)

class DataPublisher extends ActorPublisher[Int] {

  import akka.stream.actor.ActorPublisherMessage._

  var items: List[Int] = List.empty

  def receive = {
    case s: String =>
      println(s"Producer buffer size ${items.size}")
      if (totalDemand == 0)
        items = items :+ s.toInt
      else
        onNext(s.toInt)

    case Request(demand) =>
      if (demand > items.size) {
        items foreach (onNext)
        items = List.empty
      }
      else {
        val (send, keep) = items.splitAt(demand.toInt)
        items = keep
        send foreach (onNext)
      }


    case other =>
      println(s"got other $other")
  }


}

object DataPublisher {

  case class Publish(data: Data)

}

object ActorStreamExample extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  case class Tweet(i: Int)

  case class Author(name: String)

  final case class Hashtag(name: String)

  final case class Tweet2(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] =
      body.split(" ").collect { case t if t.startsWith("#") => Hashtag(t) }.toSet
  }

  val tweets: Source[Tweet, NotUsed] = Source(new immutable.Iterable[Tweet] {
    var i = 0
    override def iterator: Iterator[Tweet] = new Iterator[Tweet] {
      override def hasNext: Boolean = i != 100

      override def next(): Tweet = {
//        Thread.sleep(100)
        i = i+1
        println(s"producing $i")
        Tweet(i)
      }
    }
  })


  val tweets2: Source[Tweet2, NotUsed] = Source(
    Tweet2(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet2(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
      Tweet2(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
      Tweet2(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
      Tweet2(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
      Tweet2(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
      Tweet2(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet2(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
      Tweet2(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
      Tweet2(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
      Nil)


  def test1(): Unit = {
    val source: Source[Int, NotUsed] = Source(1 to 100)
    source.runForeach(i => println(i)).onComplete(_ => materializer.shutdown())
  }

  def lineSink(fileName: String): Sink[String, Future[IOResult]] = Flow[String].map(s => ByteString(s + "\n"))
    .toMat(FileIO.toPath(Paths.get(fileName)))(Keep.right)

  def test2(): Unit = {
    val source: Source[Int, NotUsed] = Source(1 to 100)
    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
    val result: Future[IOResult] =
      factorials
        .map(num => ByteString(s"$num\n"))
        .runWith(FileIO.toPath(Paths.get("factorials.txt")))
  }

  def test3(): Unit = {
    val source: Source[Int, NotUsed] = Source(1 to 100)
    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
    factorials.map(_.toString()).runWith(lineSink("test"))

  }

  def test4(): Unit = {
    val source: Source[Int, NotUsed] = Source(1 to 20)
    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
    factorials.zipWith(Source(1 to 10))((i, out) => s"$i=>$out").runForeach(println)

  }

  def test5(): Unit = {
    val source: Source[Int, NotUsed] = Source(1 to 20)
    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
    factorials.zip(Source(10 to 18)).throttle(1, 1 seconds, 1, ThrottleMode.Shaping).runForeach(println)
  }

  def test6() = {
    tweets.runForeach(t => println(t.i))
  }

  def subscriberCreatesDemand(): Unit = {
    val i = Sink.fromSubscriber(
      new Subscriber[Tweet] {
        var sub: Subscription = _
        var i = 10

        override def onError(t: Throwable): Unit = ???

        override def onComplete(): Unit = ???

        override def onNext(t: Tweet): Unit = {
          println(t)
          if (i >= 0) {
            sub.request(1) //demands one more from stream.
          } else {
            //no longer demands
          }
          i = i - 1
        }

        override def onSubscribe(s: Subscription): Unit = {
          sub = s
          s.request(2) //initial demand
        }
      })

    val run = tweets.toMat(i)(Keep.right)
    run.run() //totla demand is initial demand + (i+1)

  }


  def test7() = {
    val akkaTag = Hashtag("#akka")
    val authors: Source[Author, NotUsed] =
      tweets2.filter(_.hashtags.contains(akkaTag))
        .map(_.author)
    authors.runWith(Sink.foreach(println))
    //    tweets2.mapConcat(_.hashtags.toList).runForeach(println)
  }

  def slowConsumerWithOverflow(): Unit = {
    val i = Sink.fromSubscriber(
      new Subscriber[Tweet] {
        var sub: Subscription = _
        var i = 10

        override def onError(t: Throwable): Unit = ???

        override def onComplete(): Unit = ???

        override def onNext(t: Tweet): Unit = {
          println(s"Consumed $t")
//          Thread.sleep(100)
          sub.request(1) //demands one more from stream.
        }

        override def onSubscribe(s: Subscription): Unit = {
          sub = s
          s.request(1) //initial demand
        }
      })
    tweets.buffer(2,OverflowStrategy.dropTail).toMat(i)(Keep.right).run()
  }

  /**
    * Consumer consumes an element each 1 second. Producer produces every 10 ms.
    * Longer this runs more memory it consumes.
    *
    * @return
    */
  def fastPublisher_slowConsumer(): Unit = {
    val dataPublisherRef = system.actorOf(Props[DataPublisher])
    val dataPublisher = ActorPublisher[Int](dataPublisherRef)

    val r = new Runnable {
      val rand = new Random()

      override def run(): Unit = {
        while (true) {
          dataPublisherRef ! rand.nextInt().toString
          Thread.sleep(10)
        }
      }
    }
    val t = new Thread(r)
    t.start()

    Source.fromPublisher(dataPublisher).map(_ * 2).runWith(Sink.foreach(s => {
      Thread.sleep(1000)
      print(s)
    }))
  }

  slowConsumerWithOverflow()
}
