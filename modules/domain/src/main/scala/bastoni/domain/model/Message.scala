package bastoni.domain.model

import cats.implicits.showInterpolator
import cats.Show
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

import java.util.UUID
import scala.util.Try

opaque type MessageId = UUID

object MessageId:
  def newId: MessageId                       = UUID.randomUUID()
  def tryParse(s: String): Option[MessageId] = Try(unsafeParse(s)).toOption
  def unsafeParse(s: String): MessageId      = UUID.fromString(s)

  given Encoder[MessageId] = Encoder.encodeUUID
  given Decoder[MessageId] = Decoder.decodeUUID

case class Message(id: MessageId, roomId: RoomId, data: ServerEvent | Command)

object Message:

  given Show[Message] = Show(message =>
    message.data match
      case event: ServerEvent =>
        show"ServerEvent($event, ${message.roomId})"
      case command: Command =>
        show"Command($command, ${message.roomId})"
  )

  given Encoder[ServerEvent | Command] = Encoder.instance {
    case message: ServerEvent =>
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

  given dataDecoder: Decoder[ServerEvent | Command] = Decoder.instance(obj =>
    for
      t <- obj.downField("type").as[String]
      d <- if (t == "Event") obj.downField("data").as[ServerEvent] else obj.downField("data").as[Command]
    yield d
  )

  given Encoder[Message] = Encoder
    .instance(message =>
      message.data.asJson
        .mapObject(
          _.add("id", message.id.asJson)
            .add("roomId", message.roomId.asJson)
        )
    )

  given Decoder[Message] = Decoder.instance(obj =>
    for
      id     <- obj.downField("id").as[MessageId]
      roomId <- obj.downField("roomId").as[RoomId]
      data   <- dataDecoder(obj)
    yield Message(id, roomId, data)
  )
end Message
