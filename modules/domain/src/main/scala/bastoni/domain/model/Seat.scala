package bastoni.domain.model

case class DealtCard(card: Card, face: Face)

enum PlayerState:
  case Inactive, Active, Acting

case class Seat(
  player: Option[(GamePlayer, PlayerState)],
  hand: List[DealtCard],
  collected: Set[DealtCard]
)

case class Table(seats: List[Seat])
