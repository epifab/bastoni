package bastoni.domain.model

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class GamePlayer(basePlayer: Player, points: Int, dealer: Boolean = false) extends Player(basePlayer.id, basePlayer.name):
  def win: GamePlayer = copy(points = points + 1)
  def win(additionalPoints: Int): GamePlayer = copy(points = points + additionalPoints)


object GamePlayer:
  private case class GamePlayerView(id: PlayerId, name: String, points: Int)

  given Encoder[GamePlayer] = deriveEncoder[GamePlayerView].contramap(gamePlayer =>
    GamePlayerView(
      gamePlayer.id,
      gamePlayer.name,
      gamePlayer.points
    )
  )

  given Decoder[GamePlayer] = deriveDecoder[GamePlayerView].map(gamePlayer =>
    GamePlayer(
      Player(gamePlayer.id, gamePlayer.name),
      gamePlayer.points
    )
  )
