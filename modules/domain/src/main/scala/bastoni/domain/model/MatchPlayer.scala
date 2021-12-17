package bastoni.domain.model

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class MatchPlayer(gamePlayer: GamePlayer, hand: List[Card], taken: List[Card], extraPoints: Int = 0) extends Player(gamePlayer.id, gamePlayer.name):
  def has(card: Card): Boolean = hand.contains(card)
  def draw(card: Card) = copy(hand = card :: hand)
  def draw(cards: List[Card]) = copy(hand = cards ++ hand)
  def addExtraPoints(points: Int): MatchPlayer = copy(extraPoints = extraPoints + points)

  def play(card: Card): MatchPlayer =
    assert(has(card), "Players can't play cards that they don't own")
    copy(hand = hand.filterNot(_ == card))

  def take(cards: List[Card]): MatchPlayer =
    assert(!cards.exists(taken.contains), "Players can't take cards that were previously taken")
    copy(taken = taken ++ cards)

object MatchPlayer:
  private case class MatchPlayerView(id: PlayerId, name: String, points: Int, hand: List[Card], taken: List[Card])

  given Encoder[MatchPlayer] = deriveEncoder[MatchPlayerView].contramap[MatchPlayer](matchPlayer =>
    MatchPlayerView(
      matchPlayer.id,
      matchPlayer.name,
      matchPlayer.gamePlayer.points,
      matchPlayer.hand,
      matchPlayer.taken
    )
  )

  given Decoder[MatchPlayer] = deriveDecoder[MatchPlayerView].map(matchPlayer =>
    MatchPlayer(
      GamePlayer(
        Player(matchPlayer.id, matchPlayer.name),
        matchPlayer.points
      ),
      matchPlayer.hand,
      matchPlayer.taken
    )
  )
