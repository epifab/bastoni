package bastoni.domain.model

sealed trait Event

import io.circe.{Json, Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

object Event:
  sealed trait RoomEvent extends Event:
    def player: Player
    def room: Room
  case class   PlayerJoined(player: Player, room: Room) extends RoomEvent
  case class   PlayerLeft(player: Player, room: Room) extends RoomEvent

  sealed trait GameEvent extends Event
  case class   GameStarted(gameType: GameType) extends GameEvent
  case class   DeckShuffled(cards: List[Card]) extends GameEvent
  case class   CardDealt(playerId: PlayerId, card: Card, face: Face) extends GameEvent
  case class   TrumpRevealed(card: Card) extends GameEvent
  case class   CardPlayed(playerId: PlayerId, card: Card) extends GameEvent
  case class   TrickCompleted(winnerId: PlayerId) extends GameEvent
  case class   MatchPointsCount(playerIds: List[PlayerId], points: Int) extends GameEvent
  case class   GamePointsCount(playerIds: List[PlayerId], points: Int) extends GameEvent
  case class   MatchCompleted(winnerIds: List[PlayerId]) extends GameEvent
  case class   GameCompleted(winnerIds: List[PlayerId]) extends GameEvent
  case object  MatchDraw extends GameEvent
  case object  MatchAborted extends GameEvent
  case object  GameAborted extends GameEvent

  given Encoder[Event] = Encoder.instance {
    case obj: PlayerJoined     => deriveEncoder[PlayerJoined].mapJsonObject(_.add("type", "PlayerJoined".asJson))(obj)
    case obj: PlayerLeft       => deriveEncoder[PlayerLeft].mapJsonObject(_.add("type", "PlayerLeft".asJson))(obj)
    case obj: GameStarted      => deriveEncoder[GameStarted].mapJsonObject(_.add("type", "GameStarted".asJson))(obj)
    case obj: DeckShuffled     => deriveEncoder[DeckShuffled].mapJsonObject(_.add("type", "DeckShuffled".asJson))(obj)
    case obj: CardDealt        => deriveEncoder[CardDealt].mapJsonObject(_.add("type", "CardDealt".asJson))(obj)
    case obj: TrumpRevealed    => deriveEncoder[TrumpRevealed].mapJsonObject(_.add("type", "TrumpRevealed".asJson))(obj)
    case obj: CardPlayed       => deriveEncoder[CardPlayed].mapJsonObject(_.add("type", "CardPlayed".asJson))(obj)
    case obj: TrickCompleted   => deriveEncoder[TrickCompleted].mapJsonObject(_.add("type", "TrickCompleted".asJson))(obj)
    case obj: MatchPointsCount => deriveEncoder[MatchPointsCount].mapJsonObject(_.add("type", "MatchPointsCount".asJson))(obj)
    case obj: GamePointsCount  => deriveEncoder[GamePointsCount].mapJsonObject(_.add("type", "GamePointsCount".asJson))(obj)
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
    case "MatchPointsCount" => deriveDecoder[MatchPointsCount](obj)
    case "GamePointsCount"  => deriveDecoder[GamePointsCount](obj)
    case "MatchCompleted"   => deriveDecoder[MatchCompleted](obj)
    case "GameCompleted"    => deriveDecoder[GameCompleted](obj)
    case "MatchDraw"        => Right(MatchDraw)
    case "MatchAborted"     => Right(MatchAborted)
    case "GameAborted"      => Right(GameAborted)
  })
