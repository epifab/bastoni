package bastoni.domain.model

case class DealtCard(card: Card, face: Face)

case class Seat(
  player: Option[GamePlayer],
  hand: List[DealtCard],
  collected: Set[DealtCard]
)
