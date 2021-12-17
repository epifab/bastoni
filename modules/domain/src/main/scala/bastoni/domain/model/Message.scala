package bastoni.domain.model

import java.util.UUID
import scala.util.Try
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

opaque type MessageId = UUID

object MessageId:
  def newId: MessageId = UUID.randomUUID()
  def parse(s: String): Option[MessageId] = Try(UUID.fromString(s)).toOption
  given Encoder[MessageId] = Encoder[String].contramap(_.toString)
  given Decoder[MessageId] = Decoder[String].emap(parse(_).toRight("Not a valid ID"))

case class Message(id: MessageId, roomId: RoomId, data: Command | Event)

object Message:

  given Encoder[Event | Command] = Encoder.instance {
    case message: Event =>
      Json.obj(
        "type" -> "Event".asJson,
        "data" -> message.asJson
      )
    case message: Command =>
      Json.obj(
        "type" -> "Command".asJson,
        "data" -> message.asJson
      )
  }

  given dataDecoder: Decoder[Event | Command] = Decoder.instance(obj =>
    for {
      t <- obj.downField("type").as[String]
      d <- if (t == "Event") obj.downField("data").as[Event] else obj.downField("data").as[Command]
    } yield d
  )

  given Encoder[Message] = Encoder
    .instance(message =>
      message
        .data.asJson
        .mapObject(_
          .add("id", message.id.asJson)
          .add("roomId", message.roomId.asJson)
        )
    )

  given Decoder[Message] = Decoder.instance(obj =>
    for {
      id <- obj.downField("id").as[MessageId]
      roomId <- obj.downField("roomId").as[RoomId]
      data <- dataDecoder(obj)
    } yield Message(id, roomId, data)
  )
