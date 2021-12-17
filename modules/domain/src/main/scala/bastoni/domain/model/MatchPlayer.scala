package bastoni.domain.model

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class MatchPlayer(gamePlayer: GamePlayer, hand: Set[Card], collected: Set[Card]):
  def player: Player = gamePlayer.player

  val id: PlayerId = gamePlayer.id
  def is(p: Player): Boolean = p.id == id
  def is(p: PlayerId): Boolean = p == id

  def has(card: Card): Boolean = hand.contains(card)
  def draw(card: Card) = copy(hand = hand + card)

  def play(card: Card) =
    if (!has(card)) throw new IllegalArgumentException("Card not found")
    copy(hand = hand - card) -> card

  def collect(cards: Set[Card]) = copy(collected = collected ++ cards)

object MatchPlayer:
  private case class MatchPlayerView(id: PlayerId, name: String, points: Int, hand: Set[Card], collected: Set[Card])

  given Encoder[MatchPlayer] = deriveEncoder[MatchPlayerView].contramap[MatchPlayer](matchPlayer =>
    MatchPlayerView(
      matchPlayer.gamePlayer.player.id,
      matchPlayer.gamePlayer.player.name,
      matchPlayer.gamePlayer.points,
      matchPlayer.hand,
      matchPlayer.collected
    )
  )

  given Decoder[MatchPlayer] = deriveDecoder[MatchPlayerView].map(matchPlayer =>
    MatchPlayer(
      GamePlayer(
        Player(matchPlayer.id, matchPlayer.name),
        matchPlayer.points
      ),
      matchPlayer.hand,
      matchPlayer.collected
    )
  )
