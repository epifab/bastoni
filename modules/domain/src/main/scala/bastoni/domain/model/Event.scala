package bastoni.domain.model

import bastoni.domain.logic.{briscola, tressette, scopa}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}

sealed trait Event

sealed trait ServerEvent extends Event
sealed trait PlayerEvent extends Event
sealed trait PublicEvent extends ServerEvent with PlayerEvent


object Event:
  // Public events
  case class  PlayerJoinedRoom(user: User, seat: Int) extends PublicEvent
  case class  PlayerLeftRoom(user: User, seat: Int) extends PublicEvent
  case class  MatchStarted(gameType: GameType, matchScores: List[MatchScore]) extends PublicEvent
  case class  TrumpRevealed(card: VisibleCard) extends PublicEvent
  case class  BoardCardsDealt(cards: List[VisibleCard]) extends PublicEvent
  case class  CardPlayed(playerId: UserId, card: VisibleCard) extends PublicEvent
  case class  CardsTaken(playerId: UserId, taken: List[VisibleCard], scopa: Option[VisibleCard]) extends PublicEvent
  case class  ActionRequested(playerId: UserId, action: Action, timeout: Option[Timeout.Active]) extends PublicEvent
  case class  TimedOut(playerId: UserId, action: Action) extends PublicEvent
  case class  TrickCompleted(winnerId: UserId) extends PublicEvent

  case class  GameCompleted(scores: List[GameScore], matchScores: List[MatchScore]) extends PublicEvent
  case class  MatchCompleted(winnerIds: List[UserId]) extends PublicEvent
  case object GameAborted extends PublicEvent
  case object MatchAborted extends PublicEvent

  sealed trait CardsDealt[C <: CardView]:
    def playerId: UserId
    def cards: List[C]

  case class CardsDealtServerView(playerId: UserId, cards: List[CardServerView]) extends CardsDealt[CardServerView] with ServerEvent
  case class CardsDealtPlayerView(playerId: UserId, cards: List[CardPlayerView]) extends CardsDealt[CardPlayerView] with PlayerEvent

  object CardsDealt:
    def apply(playerId: UserId, cards: List[VisibleCard], facing: Direction): CardsDealtServerView =
      CardsDealtServerView(playerId, cards.map(card => CardServerView(card, facing)))

    def apply(playerId: UserId, cards: List[CardInstance]): CardsDealtPlayerView =
      CardsDealtPlayerView(playerId, cards.map(CardPlayerView(_)))

  sealed trait DeckShuffled
  case class DeckShuffledServerView(cards: List[VisibleCard]) extends DeckShuffled with ServerEvent
  case class DeckShuffledPlayerView(numberOfCards: Int) extends DeckShuffled with PlayerEvent

  object DeckShuffled:
    def apply(deck: Deck): DeckShuffledServerView = DeckShuffledServerView(deck.cards)
    def apply(cards: List[VisibleCard]): DeckShuffledServerView = DeckShuffledServerView(cards)
    def apply(numberOfCards: Int): DeckShuffledPlayerView = DeckShuffledPlayerView(numberOfCards)

  case class Snapshot(room: RoomServerView) extends ServerEvent

  given publicEventEncoder: Encoder[PublicEvent] = Encoder.instance {
    case obj: PlayerJoinedRoom  => deriveEncoder[PlayerJoinedRoom].mapJsonObject(_.add("type", "PlayerJoinedRoom".asJson))(obj)
    case obj: PlayerLeftRoom    => deriveEncoder[PlayerLeftRoom].mapJsonObject(_.add("type", "PlayerLeftRoom".asJson))(obj)
    case obj: MatchStarted      => deriveEncoder[MatchStarted].mapJsonObject(_.add("type", "MatchStarted".asJson))(obj)
    case obj: ActionRequested   => deriveEncoder[ActionRequested].mapJsonObject(_.add("type", "ActionRequested".asJson))(obj)
    case obj: TimedOut          => deriveEncoder[TimedOut].mapJsonObject(_.add("type", "TimedOut".asJson))(obj)
    case obj: TrumpRevealed     => deriveEncoder[TrumpRevealed].mapJsonObject(_.add("type", "TrumpRevealed".asJson))(obj)
    case obj: BoardCardsDealt   => deriveEncoder[BoardCardsDealt].mapJsonObject(_.add("type", "BoardCardsDealt".asJson))(obj)
    case obj: CardPlayed        => deriveEncoder[CardPlayed].mapJsonObject(_.add("type", "CardPlayed".asJson))(obj)
    case obj: CardsTaken        => deriveEncoder[CardsTaken].mapJsonObject(_.add("type", "CardsTaken".asJson))(obj)
    case obj: TrickCompleted    => deriveEncoder[TrickCompleted].mapJsonObject(_.add("type", "TrickCompleted".asJson))(obj)

    case obj: GameCompleted     => deriveEncoder[GameCompleted].mapJsonObject(_.add("type", "GameCompleted".asJson))(obj)
    case obj: MatchCompleted    => deriveEncoder[MatchCompleted].mapJsonObject(_.add("type", "MatchCompleted".asJson))(obj)
    case GameAborted            => Json.obj("type" -> "GameAborted".asJson)
    case MatchAborted           => Json.obj("type" -> "MatchAborted".asJson)
  }

  given serverEventEncoder: Encoder[ServerEvent] = Encoder.instance {
    case obj: DeckShuffledServerView => deriveEncoder[DeckShuffledServerView].mapJsonObject(_.add("type", "DeckShuffled".asJson))(obj)
    case obj: CardsDealtServerView   => deriveEncoder[CardsDealtServerView].mapJsonObject(_.add("type", "CardsDealt".asJson))(obj)
    case obj: Snapshot               => deriveEncoder[Snapshot].mapJsonObject(_.add("type", "Snapshot".asJson))(obj)
    case obj: PublicEvent            => publicEventEncoder(obj)
  }

  given playerEventEncoder: Encoder[PlayerEvent] = Encoder.instance {
    case obj: DeckShuffledPlayerView => deriveEncoder[DeckShuffledPlayerView].mapJsonObject(_.add("type", "DeckShuffled".asJson))(obj)
    case obj: CardsDealtPlayerView   => deriveEncoder[CardsDealtPlayerView].mapJsonObject(_.add("type", "CardsDealt".asJson))(obj)
    case obj: PublicEvent            => publicEventEncoder(obj)
  }

  given publicEventDecoder: Decoder[PublicEvent] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "PlayerJoinedRoom"  => deriveDecoder[PlayerJoinedRoom](obj)
    case "PlayerLeftRoom"    => deriveDecoder[PlayerLeftRoom](obj)
    case "MatchStarted"      => deriveDecoder[MatchStarted](obj)
    case "ActionRequested"   => deriveDecoder[ActionRequested](obj)
    case "TimedOut"          => deriveDecoder[TimedOut](obj)
    case "TrumpRevealed"     => deriveDecoder[TrumpRevealed](obj)
    case "BoardCardsDealt"   => deriveDecoder[BoardCardsDealt](obj)
    case "CardPlayed"        => deriveDecoder[CardPlayed](obj)
    case "CardsTaken"        => deriveDecoder[CardsTaken](obj)
    case "TrickCompleted"    => deriveDecoder[TrickCompleted](obj)

    case "GameCompleted"     => deriveDecoder[GameCompleted](obj)
    case "MatchCompleted"    => deriveDecoder[MatchCompleted](obj)
    case "GameAborted"       => Right(GameAborted)
    case "MatchAborted"      => Right(MatchAborted)
  })

  given serverEventDecoder: Decoder[ServerEvent] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "DeckShuffled" => deriveDecoder[DeckShuffledServerView](obj)
    case "CardsDealt"   => deriveDecoder[CardsDealtServerView](obj)
    case "Snapshot"     => deriveDecoder[Snapshot](obj)
    case somethingElse  => publicEventDecoder(obj)
  })

  given playerEventDecoder: Decoder[PlayerEvent] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "DeckShuffled" => deriveDecoder[DeckShuffledPlayerView](obj)
    case "CardsDealt"   => deriveDecoder[CardsDealtPlayerView](obj)
    case somethingElse  => publicEventDecoder(obj)
  })
