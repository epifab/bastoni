package bastoni.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

sealed trait PlayerState extends User:
  def sitOut: PlayerState.SittingOut

object PlayerState:
  case class SittingOut(user: User) extends PlayerState with User(user.id, user.name):
    def sitIn: Waiting     = Waiting(MatchPlayer(user, points = 0))
    def sitOut: SittingOut = this

  sealed abstract class SittingIn(val player: MatchPlayer) extends PlayerState with User(player.id, player.name):
    def mapPlayer(f: MatchPlayer => MatchPlayer): SittingIn
    def sitOut: SittingOut                                    = SittingOut(player.user)
    def act(action: Action, timeout: Option[Timeout]): Acting = Acting(player, action, timeout)

  case class Waiting(override val player: MatchPlayer) extends SittingIn(player):
    def mapPlayer(f: MatchPlayer => MatchPlayer): Waiting = copy(player = f(player))

  case class Acting(override val player: MatchPlayer, action: Action, timeout: Option[Timeout])
      extends SittingIn(player):
    def mapPlayer(f: MatchPlayer => MatchPlayer): Acting = copy(player = f(player))
    def done: Waiting                                    = Waiting(player)
    def playing: Boolean = action match
      case Action.PlayCard(_) => true
      case _                  => false

  case class EndOfGame(override val player: MatchPlayer, points: Int, winner: Boolean) extends SittingIn(player):
    def mapPlayer(f: MatchPlayer => MatchPlayer): EndOfGame = copy(player = f(player))

  case class EndOfMatch(override val player: MatchPlayer, winner: Boolean) extends SittingIn(player):
    def mapPlayer(f: MatchPlayer => MatchPlayer): EndOfMatch = copy(player = f(player))

  given Encoder[PlayerState] = ConfiguredEncoder.derive(discriminator = Some("state"))
  given Decoder[PlayerState] = ConfiguredDecoder.derive(discriminator = Some("state"))

end PlayerState
