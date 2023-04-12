package bastoni.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class MatchPlayer(user: User, points: Int) extends User(user.id, user.name):
  def win: MatchPlayer                        = copy(points = points + 1)
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
