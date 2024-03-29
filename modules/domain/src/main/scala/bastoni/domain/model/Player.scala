package bastoni.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Player(matchPlayer: MatchPlayer, hand: List[VisibleCard], pile: List[VisibleCard], extraPoints: Int = 0)
    extends User(matchPlayer.id, matchPlayer.name):
  def has(card: VisibleCard): Boolean        = hand.contains(card)
  def draw(card: VisibleCard): Player        = copy(hand = card :: hand)
  def draw(cards: List[VisibleCard]): Player = copy(hand = cards ++ hand)
  def addExtraPoints(points: Int): Player    = copy(extraPoints = extraPoints + points)

  def play(card: VisibleCard): Player =
    assert(has(card), "Players can't play cards that they don't own")
    copy(hand = hand.filterNot(_ == card))

  def take(cards: List[VisibleCard]): Player =
    assert(!cards.exists(pile.contains), "Players can't take cards that were previously taken")
    copy(pile = pile ++ cards)

object Player:
  private case class EncodablePlayer(
      id: UserId,
      name: String,
      points: Int,
      hand: List[VisibleCard],
      pile: List[VisibleCard],
      extraPoints: Int
  )

  given Encoder[Player] = deriveEncoder[EncodablePlayer].contramap[Player](player =>
    EncodablePlayer(
      player.id,
      player.name,
      player.matchPlayer.points,
      player.hand,
      player.pile,
      player.extraPoints
    )
  )

  given Decoder[Player] = deriveDecoder[EncodablePlayer].map(player =>
    Player(
      MatchPlayer(
        User(player.id, player.name),
        player.points
      ),
      player.hand,
      player.pile,
      player.extraPoints
    )
  )
end Player
