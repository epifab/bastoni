package bastoni.domain.model

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class MatchPlayer(basePlayer: User, points: Int, dealer: Boolean = false) extends User(basePlayer.id, basePlayer.name):
  def win: MatchPlayer = copy(points = points + 1)
  def win(additionalPoints: Int): MatchPlayer = copy(points = points + additionalPoints)


object MatchPlayer:
  private case class GamePlayerView(id: UserId, name: String, points: Int)

  given Encoder[MatchPlayer] = deriveEncoder[GamePlayerView].contramap(player =>
    GamePlayerView(
      player.id,
      player.name,
      player.points
    )
  )

  given Decoder[MatchPlayer] = deriveDecoder[GamePlayerView].map(player =>
    MatchPlayer(
      User(player.id, player.name),
      player.points
    )
  )
