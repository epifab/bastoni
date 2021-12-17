package bastoni.domain

trait Command:
  def player: Player

case class JoinRoom(player: Player) extends Command
case class LeaveRoom(player: Player) extends Command
case class PlayCard(player: Player, card: Card) extends Command
case class DrawCard(player: Player) extends Command
