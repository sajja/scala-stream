package kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.{Config, ConfigFactory}
import examples.MyExamples.Doc
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.util.Random


object TestKafkaProducer {
  val data = List("Sajith:Hello", "Silva:World")
  val r = Random

  def make(): Stream[String] = {
    Stream.cons(data(r.nextInt(2)), make)
  }

  val config: Config = ConfigFactory.load()

  def generateDoc() = {
    val allEndPoints = List("21G", "BASWARE", "1stBP", "HansaPrint")
    Doc(Random.nextInt(1000), allEndPoints(Random.nextInt(3)))
  }

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("api")
    implicit val materializer = ActorMaterializer()
    val db = new DB
    val data = List("Sajith:Hello", "Silva:World")


    val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092").withProperty(ConsumerConfig.GROUP_ID_CONFIG,"100")
    val r = Random

    val done = Source(1 to Int.MaxValue)
      .map(_.toString)
      .map { elem =>
        Thread.sleep(1000)
        val data = generateDoc().toString()
        println(data)
        new ProducerRecord[Array[Byte], String]("test", data)
      }
      .runWith(Producer.plainSink[Array[Byte], String](producerSettings))
  }
}
