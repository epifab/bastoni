package bastoni.backend

import bastoni.domain.{Card, Player, PlayerId}

case class MatchPlayer(gamePlayer: GamePlayer, hand: Set[Card], collected: Set[Card]):
  def player: Player = gamePlayer.player

  val id: PlayerId = gamePlayer.id
  def is(p: Player): Boolean = p.id == id

  def has(card: Card): Boolean = hand.contains(card)
  def draw(card: Card) = copy(hand = hand + card)

  def play(card: Card) =
    if (!has(card)) throw new IllegalArgumentException("Card not found")
    copy(hand = hand - card) -> card

  def collect(cards: Set[Card]) = copy(collected = collected ++ cards)
