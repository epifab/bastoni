package bastoni.domain.model

case class GamePlayer(player: Player, points: Int):
  val id: PlayerId = player.id
  def is(p: Player): Boolean = p.id == id
  def is(p: PlayerId): Boolean = p == id

  def win: GamePlayer = copy(points = points + 1)
  def win(additionalPoints: Int): GamePlayer = copy(points = points + additionalPoints)
