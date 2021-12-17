package bastoni.domain.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}

sealed trait Event

sealed trait ServerEvent extends Event
sealed trait PlayerEvent extends Event
sealed trait PublicEvent extends ServerEvent with PlayerEvent

case class PointsCount(playerIds: List[PlayerId], points: Int)

object PointsCount:
  given Encoder[PointsCount] = deriveEncoder
  given Decoder[PointsCount] = deriveDecoder


object Event:
  // Public events
  case class  PlayerJoinedTable(player: Player, seat: Int) extends PublicEvent
  case class  PlayerLeftTable(player: Player, seat: Int) extends PublicEvent
  case class  GameStarted(gameType: GameType) extends PublicEvent
  case class  TrumpRevealed(card: Card) extends PublicEvent
  case class  CardPlayed(playerId: PlayerId, card: Card) extends PublicEvent
  case class  ActionRequested(playerId: PlayerId, action: Action) extends PublicEvent
  case class  TrickCompleted(winnerId: PlayerId) extends PublicEvent
  case class  MatchCompleted(winnerIds: List[PlayerId], matchPoints: List[PointsCount], gamePoints: List[PointsCount]) extends PublicEvent
  case class  GameCompleted(winnerIds: List[PlayerId]) extends PublicEvent
  case object MatchAborted extends PublicEvent
  case object GameAborted extends PublicEvent

  sealed trait CardDealt[C <: CardView]:
    def playerId: PlayerId
    def card: C

  case class CardDealtServerPOV(playerId: PlayerId, card: CardServerView) extends CardDealt[CardServerView] with ServerEvent
  case class CardDealtPlayerPOV(playerId: PlayerId, card: CardPlayerView) extends CardDealt[CardPlayerView] with PlayerEvent

  object CardDealt:
    def apply(playerId: PlayerId, card: Card, face: Face): CardDealtServerPOV =
      CardDealtServerPOV(playerId, CardServerView(card, face))

    def apply(playerId: PlayerId, card: Option[Card]): CardDealtPlayerPOV =
      CardDealtPlayerPOV(playerId, CardPlayerView(card))

  sealed trait DeckShuffled
  case class DeckShuffledServerPOV(cards: List[Card]) extends DeckShuffled with ServerEvent
  case class DeckShuffledPlayerPOV(numberOfCards: Int) extends DeckShuffled with PlayerEvent

  object DeckShuffled:
    def apply(cards: List[Card]): DeckShuffledServerPOV =
      DeckShuffledServerPOV(cards)

    def apply(numberOfCards: Int): DeckShuffledPlayerPOV =
      DeckShuffledPlayerPOV(numberOfCards)

  case class Snapshot(table: TableServerView) extends ServerEvent

  given publicEventEncoder: Encoder[PublicEvent] = Encoder.instance {
    case obj: PlayerJoinedTable => deriveEncoder[PlayerJoinedTable].mapJsonObject(_.add("type", "PlayerJoinedTable".asJson))(obj)
    case obj: PlayerLeftTable   => deriveEncoder[PlayerLeftTable].mapJsonObject(_.add("type", "PlayerLeftTable".asJson))(obj)
    case obj: GameStarted       => deriveEncoder[GameStarted].mapJsonObject(_.add("type", "GameStarted".asJson))(obj)
    case obj: ActionRequested   => deriveEncoder[ActionRequested].mapJsonObject(_.add("type", "ActionRequested".asJson))(obj)
    case obj: TrumpRevealed     => deriveEncoder[TrumpRevealed].mapJsonObject(_.add("type", "TrumpRevealed".asJson))(obj)
    case obj: CardPlayed        => deriveEncoder[CardPlayed].mapJsonObject(_.add("type", "CardPlayed".asJson))(obj)
    case obj: TrickCompleted    => deriveEncoder[TrickCompleted].mapJsonObject(_.add("type", "TrickCompleted".asJson))(obj)
    case obj: MatchCompleted    => deriveEncoder[MatchCompleted].mapJsonObject(_.add("type", "MatchCompleted".asJson))(obj)
    case obj: GameCompleted     => deriveEncoder[GameCompleted].mapJsonObject(_.add("type", "GameCompleted".asJson))(obj)
    case MatchAborted           => Json.obj("type" -> "MatchAborted".asJson)
    case GameAborted            => Json.obj("type" -> "GameAborted".asJson)
  }

  given serverEventEncoder: Encoder[ServerEvent] = Encoder.instance {
    case obj: DeckShuffledServerPOV  => deriveEncoder[DeckShuffledServerPOV].mapJsonObject(_.add("type", "DeckShuffled".asJson))(obj)
    case obj: CardDealtServerPOV     => deriveEncoder[CardDealtServerPOV].mapJsonObject(_.add("type", "CardDealt".asJson))(obj)
    case obj: Snapshot               => deriveEncoder[Snapshot].mapJsonObject(_.add("type", "Snapshot".asJson))(obj)
    case obj: PublicEvent            => publicEventEncoder(obj)
  }

  given playerEventEncoder: Encoder[PlayerEvent] = Encoder.instance {
    case obj: DeckShuffledPlayerPOV  => deriveEncoder[DeckShuffledPlayerPOV].mapJsonObject(_.add("type", "DeckShuffled".asJson))(obj)
    case obj: CardDealtPlayerPOV     => deriveEncoder[CardDealtPlayerPOV].mapJsonObject(_.add("type", "CardDealt".asJson))(obj)
    case obj: PublicEvent            => publicEventEncoder(obj)
  }

  given publicEventDecoder: Decoder[PublicEvent] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "PlayerJoinedTable" => deriveDecoder[PlayerJoinedTable](obj)
    case "PlayerLeftTable"   => deriveDecoder[PlayerLeftTable](obj)
    case "GameStarted"       => deriveDecoder[GameStarted](obj)
    case "ActionRequested"   => deriveDecoder[ActionRequested](obj)
    case "TrumpRevealed"     => deriveDecoder[TrumpRevealed](obj)
    case "CardPlayed"        => deriveDecoder[CardPlayed](obj)
    case "TrickCompleted"    => deriveDecoder[TrickCompleted](obj)
    case "MatchCompleted"    => deriveDecoder[MatchCompleted](obj)
    case "GameCompleted"     => deriveDecoder[GameCompleted](obj)
    case "MatchAborted"      => Right(MatchAborted)
    case "GameAborted"       => Right(GameAborted)
  })

  given serverEventDecoder: Decoder[ServerEvent] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "DeckShuffled" => deriveDecoder[DeckShuffledServerPOV](obj)
    case "CardDealt"    => deriveDecoder[CardDealtServerPOV](obj)
    case "Snapshot"     => deriveDecoder[Snapshot](obj)
    case somethingElse  => publicEventDecoder(obj)
  })

  given playerEventDecoder: Decoder[PlayerEvent] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "DeckShuffled" => deriveDecoder[DeckShuffledPlayerPOV](obj)
    case "CardDealt"    => deriveDecoder[CardDealtPlayerPOV](obj)
    case somethingElse  => publicEventDecoder(obj)
  })
