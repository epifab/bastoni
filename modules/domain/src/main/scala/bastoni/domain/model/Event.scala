package bastoni.domain.model

import bastoni.domain.logic.{briscola, scopa, tressette}
import io.circe.*
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.syntax.*

sealed trait Event

// ServerEvent are meant to be consumed by the server only
// those might include information that is not safe to share with the clients
sealed trait ServerEvent extends Event

// PlayerEvent are meant to be consumed by the client only
// those might include incomplete information and refer to a specific player view of a game
sealed trait PlayerEvent extends Event

// Public events are the intersection between server and player events
// as such, they can be shared safely with any client and can be consumed by the server
sealed trait PublicEvent extends ServerEvent, PlayerEvent

object Event:
  // Public events
  case class PlayerJoinedRoom(user: User, seat: Int)                                            extends PublicEvent
  case class PlayerLeftRoom(user: User, seat: Int)                                              extends PublicEvent
  case class MatchStarted(gameType: GameType, matchScores: List[MatchScore])                    extends PublicEvent
  case class TrumpRevealed(card: VisibleCard)                                                   extends PublicEvent
  case class BoardCardsDealt(cards: List[VisibleCard])                                          extends PublicEvent
  case class CardPlayed(playerId: UserId, card: VisibleCard)                                    extends PublicEvent
  case class CardsTaken(playerId: UserId, taken: List[VisibleCard], scopa: Option[VisibleCard]) extends PublicEvent
  case class PlayerConfirmed(playerId: UserId)                                                  extends PublicEvent
  case class TimedOut(playerId: UserId, action: Action)                                         extends PublicEvent
  case class TrickCompleted(winnerId: UserId)                                                   extends PublicEvent
  case class GameCompleted(scores: List[GameScore], matchScores: List[MatchScore])              extends PublicEvent
  case class MatchCompleted(winnerIds: List[UserId])                                            extends PublicEvent
  case class GameAborted(reason: GameAborted.Reason)                                            extends PublicEvent
  case class MatchAborted(reason: GameAborted.Reason)                                           extends PublicEvent

  object GameAborted:
    opaque type Reason = String

    object Reason:
      val playerLeftTheRoom: Reason         = "player-left-the-room"
      val playerTimeout: Reason             = "player-timeout"
      val unexpectedNumberOfPlayers: Reason = "unexpected-number-of-players"

      given Codec[Reason] = Codec.from(Decoder.decodeString, Encoder.encodeString)

  sealed trait CardsDealt[C <: CardView]:
    def playerId: UserId
    def cards: List[C]

  object CardsDealt:
    def apply(playerId: UserId, cards: List[VisibleCard], facing: Direction): ServerOnlyEvent.CardsDealt =
      ServerOnlyEvent.CardsDealt(playerId, cards.map(card => CardServerView(card, facing)))

    def apply(playerId: UserId, cards: List[CardInstance]): PlayerOnlyEvent.CardsDealt =
      PlayerOnlyEvent.CardsDealt(playerId, cards.map(CardPlayerView(_)))

  sealed trait DeckShuffled

  object DeckShuffled:
    def apply(deck: Deck): ServerOnlyEvent.DeckShuffled = ServerOnlyEvent.DeckShuffled(deck)

    def apply(numberOfCards: Int): PlayerOnlyEvent.DeckShuffled = PlayerOnlyEvent.DeckShuffled(numberOfCards)

  /** ServerView includes all events that contain sensitive information and are not safe to be shared between all
    * players
    */
  object ServerOnlyEvent:
    case class CardsDealt(playerId: UserId, cards: List[CardServerView])
        extends Event.CardsDealt[CardServerView]
        with ServerEvent

    case class DeckShuffled(deck: Deck) extends Event.DeckShuffled with ServerEvent

  /** PlayerView includes all events that a player would see for the corresponding server side event.
    */
  object PlayerOnlyEvent:
    case class CardsDealt(playerId: UserId, cards: List[CardPlayerView])
        extends Event.CardsDealt[CardPlayerView]
        with PlayerEvent

    case class DeckShuffled(numberOfCards: Int) extends Event.DeckShuffled with PlayerEvent

  case class PlayerConnected(user: User, room: RoomServerView) extends ServerEvent

  given publicEventEncoder: Encoder[PublicEvent] =
    ConfiguredEncoder.derive(discriminator = Some("eventType"))

  given publicEventDecoder: Decoder[PublicEvent] =
    ConfiguredDecoder.derive(discriminator = Some("eventType"))

  given serverEventEncoder: Encoder[ServerEvent] =
    ConfiguredEncoder.derive(discriminator = Some("eventType"))

  given serverEventDecoder: Decoder[ServerEvent] =
    ConfiguredDecoder.derive(discriminator = Some("eventType"))

  given playerEventEncoder: Encoder[PlayerEvent] =
    ConfiguredEncoder.derive(discriminator = Some("eventType"))

  given playerEventDecoder: Decoder[PlayerEvent] =
    ConfiguredDecoder.derive(discriminator = Some("eventType"))

end Event
