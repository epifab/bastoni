package bastoni.domain.model

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class GamePlayer(player: Player, points: Int):
  val id: PlayerId = player.id
  def is(p: Player): Boolean = p.id == id
  def is(p: PlayerId): Boolean = p == id

  def win: GamePlayer = copy(points = points + 1)
  def win(additionalPoints: Int): GamePlayer = copy(points = points + additionalPoints)


object GamePlayer:
  private case class GamePlayerView(id: PlayerId, name: String, points: Int)

  given Encoder[GamePlayer] = deriveEncoder[GamePlayerView].contramap(gamePlayer =>
    GamePlayerView(
      gamePlayer.player.id,
      gamePlayer.player.name,
      gamePlayer.points
    )
  )

  given Decoder[GamePlayer] = deriveDecoder[GamePlayerView].map(gamePlayer =>
    GamePlayer(
      Player(gamePlayer.id, gamePlayer.name),
      gamePlayer.points
    )
  )
