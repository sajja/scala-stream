package logpublisher

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{HttpCharsets, HttpEntity, MediaType}
import spray.json.DefaultJsonProtocol

/**
  * Created by sajith on 5/30/17.
  */
trait UserProtocol extends DefaultJsonProtocol {

  import spray.json._

  implicit val userFormat = jsonFormat2(User)

  val json =
    MediaType.applicationWithFixedCharset("json", HttpCharsets.`UTF-8`)

  val plain = MediaType.text("plain") withCharset HttpCharsets.`UTF-8`

  implicit def userMarshaller: ToEntityMarshaller[User] = Marshaller.oneOf(
    Marshaller.withFixedContentType(plain) { organisation =>
      HttpEntity(plain, organisation.toJson.compactPrint)
    })
}
