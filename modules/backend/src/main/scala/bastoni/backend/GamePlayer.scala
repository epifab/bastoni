package bastoni.backend

import bastoni.domain.{Card, Player, PlayerId}

case class GamePlayer(player: Player, points: Int):
  val id: PlayerId = player.id
  def is(p: Player): Boolean = p.id == id

  def win: GamePlayer = copy(points = points + 1)
