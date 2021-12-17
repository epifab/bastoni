package bastoni.domain.model

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class Player(matchPlayer: MatchPlayer, hand: List[Card], taken: List[Card], extraPoints: Int = 0) extends User(matchPlayer.id, matchPlayer.name):
  def has(card: Card): Boolean = hand.contains(card)
  def draw(card: Card) = copy(hand = card :: hand)
  def draw(cards: List[Card]) = copy(hand = cards ++ hand)
  def addExtraPoints(points: Int): Player = copy(extraPoints = extraPoints + points)

  def play(card: Card): Player =
    assert(has(card), "Players can't play cards that they don't own")
    copy(hand = hand.filterNot(_ == card))

  def take(cards: List[Card]): Player =
    assert(!cards.exists(taken.contains), "Players can't take cards that were previously taken")
    copy(taken = taken ++ cards)

object Player:
  private case class EncodablePlayer(id: UserId, name: String, points: Int, hand: List[Card], taken: List[Card])

  given Encoder[Player] = deriveEncoder[EncodablePlayer].contramap[Player](player =>
    EncodablePlayer(
      player.id,
      player.name,
      player.matchPlayer.points,
      player.hand,
      player.taken
    )
  )

  given Decoder[Player] = deriveDecoder[EncodablePlayer].map(player =>
    Player(
      MatchPlayer(
        User(player.id, player.name),
        player.points
      ),
      player.hand,
      player.taken
    )
  )
