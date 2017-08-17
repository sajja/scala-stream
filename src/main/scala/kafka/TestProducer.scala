package kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.util.Random


object TestProducer1 {
  val data = List("Sajith:Hello", "Silva:World")
  val r = Random

  def make(): Stream[String] = {
    Stream.cons(data(r.nextInt(2)), make)
  }

  val config: Config = ConfigFactory.load()

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("api")
    implicit val materializer = ActorMaterializer()
    val db = new DB
    val data = List("Sajith:Hello", "Silva:World")


    val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")
    val r = Random

    val done = Source(1 to Int.MaxValue)
      .map(_.toString)
      .map { elem =>
        Thread.sleep(1000)
        new ProducerRecord[Array[Byte], String]("test", data(r.nextInt(2)))
      }
      .runWith(Producer.plainSink(producerSettings))
  }
}
