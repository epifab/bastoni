package bastoni.domain.model

sealed trait Event

import io.circe.{Json, Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

enum Face:
  case Up, Down

object Face:
  given Encoder[Face] = Encoder[String].contramap(_.toString)
  given Decoder[Face] = Decoder[String].map(Face.valueOf)

object Event:
  case class  PlayerJoined(player: Player, room: Room) extends Event
  case class  PlayerLeft(player: Player, room: Room) extends Event
  case class  GameStarted(gameType: GameType) extends Event
  case class  DeckShuffled(seed: Int) extends Event
  case class  CardDealt(playerId: PlayerId, card: Card, face: Face = Face.Down) extends Event
  case class  TrumpRevealed(card: Card) extends Event
  case class  CardPlayed(playerId: PlayerId, card: Card) extends Event
  case class  TrickCompleted(winnerId: PlayerId) extends Event
  case class  PointsCount(playerIds: List[PlayerId], points: Int) extends Event
  case class  TotalPointsCount(playerIds: List[PlayerId], points: Int) extends Event
  case class  MatchCompleted(winnerIds: List[PlayerId]) extends Event
  case object MatchDraw extends Event
  case object MatchAborted extends Event
  case class  GameCompleted(winnerIds: List[PlayerId]) extends Event
  case object GameAborted extends Event

  given Encoder[Event] = Encoder.instance {
    case obj: PlayerJoined     => deriveEncoder[PlayerJoined].mapJsonObject(_.add("type", "PlayerJoined".asJson))(obj)
    case obj: PlayerLeft       => deriveEncoder[PlayerLeft].mapJsonObject(_.add("type", "PlayerLeft".asJson))(obj)
    case obj: GameStarted      => deriveEncoder[GameStarted].mapJsonObject(_.add("type", "GameStarted".asJson))(obj)
    case obj: DeckShuffled     => deriveEncoder[DeckShuffled].mapJsonObject(_.add("type", "DeckShuffled".asJson))(obj)
    case obj: CardDealt        => deriveEncoder[CardDealt].mapJsonObject(_.add("type", "CardDealt".asJson))(obj)
    case obj: TrumpRevealed    => deriveEncoder[TrumpRevealed].mapJsonObject(_.add("type", "TrumpRevealed".asJson))(obj)
    case obj: CardPlayed       => deriveEncoder[CardPlayed].mapJsonObject(_.add("type", "CardPlayed".asJson))(obj)
    case obj: TrickCompleted   => deriveEncoder[TrickCompleted].mapJsonObject(_.add("type", "TrickCompleted".asJson))(obj)
    case obj: PointsCount      => deriveEncoder[PointsCount].mapJsonObject(_.add("type", "PointsCount".asJson))(obj)
    case obj: TotalPointsCount => deriveEncoder[TotalPointsCount].mapJsonObject(_.add("type", "TotalPointsCount".asJson))(obj)
    case obj: MatchCompleted   => deriveEncoder[MatchCompleted].mapJsonObject(_.add("type", "MatchCompleted".asJson))(obj)
    case obj: GameCompleted    => deriveEncoder[GameCompleted].mapJsonObject(_.add("type", "GameCompleted".asJson))(obj)
    case MatchDraw             => Json.obj("type" -> "MatchDraw".asJson)
    case MatchAborted          => Json.obj("type" -> "MatchAborted".asJson)
    case GameAborted           => Json.obj("type" -> "GameAborted".asJson)
  }

  given Decoder[Event] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "PlayerJoined"     => deriveDecoder[PlayerJoined](obj)
    case "PlayerLeft"       => deriveDecoder[PlayerLeft](obj)
    case "GameStarted"      => deriveDecoder[GameStarted](obj)
    case "DeckShuffled"     => deriveDecoder[DeckShuffled](obj)
    case "CardDealt"        => deriveDecoder[CardDealt](obj)
    case "TrumpRevealed"    => deriveDecoder[TrumpRevealed](obj)
    case "CardPlayed"       => deriveDecoder[CardPlayed](obj)
    case "TrickCompleted"   => deriveDecoder[TrickCompleted](obj)
    case "PointsCount"      => deriveDecoder[PointsCount](obj)
    case "TotalPointsCount" => deriveDecoder[TotalPointsCount](obj)
    case "MatchCompleted"   => deriveDecoder[MatchCompleted](obj)
    case "GameCompleted"    => deriveDecoder[GameCompleted](obj)
    case "MatchDraw"        => Right(MatchDraw)
    case "MatchAborted"     => Right(MatchAborted)
    case "GameAborted"      => Right(GameAborted)
  })
