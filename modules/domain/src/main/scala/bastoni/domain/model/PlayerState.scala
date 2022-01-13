package bastoni.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

sealed trait PlayerState extends User:
  def sitIn: PlayerState.SittingIn
  def sitOut: PlayerState.SittingOut

object PlayerState:
  case class SittingOut(user: User) extends PlayerState with User(user.id, user.name):
    def sitIn: WaitingPlayer = WaitingPlayer(MatchPlayer(user, points = 0))
    def sitOut: SittingOut = this

  sealed abstract class SittingIn(val player: MatchPlayer) extends PlayerState with User(player.id, player.name):
    def mapPlayer(f: MatchPlayer => MatchPlayer): SittingIn
    def sitIn: SittingIn = WaitingPlayer(player)
    def sitOut: SittingOut = SittingOut(player.basePlayer)
    def act(action: Action, timeout: Option[Timeout]): ActingPlayer = ActingPlayer(player, action, timeout)

  case class WaitingPlayer(override val player: MatchPlayer) extends SittingIn(player):
    def mapPlayer(f: MatchPlayer => MatchPlayer): WaitingPlayer  = copy(player = f(player))

  case class ActingPlayer(override val player: MatchPlayer, action: Action, timeout: Option[Timeout]) extends SittingIn(player):
    def mapPlayer(f: MatchPlayer => MatchPlayer): ActingPlayer  = copy(player = f(player))
    def done: WaitingPlayer = WaitingPlayer(player)

  case class EndOfGamePlayer(override val player: MatchPlayer, points: Int, winner: Boolean) extends SittingIn(player):
    def mapPlayer(f: MatchPlayer => MatchPlayer): EndOfGamePlayer  = copy(player = f(player))

  case class EndOfMatchPlayer(override val player: MatchPlayer, winner: Boolean) extends SittingIn(player):
    def mapPlayer(f: MatchPlayer => MatchPlayer): EndOfMatchPlayer = copy(player = f(player))

  given Encoder[PlayerState] = Encoder.instance {
    case obj: SittingOut       => deriveEncoder[SittingOut].mapJsonObject(_.add("state", "SittingOut".asJson))(obj)
    case obj: WaitingPlayer    => deriveEncoder[WaitingPlayer].mapJsonObject(_.add("state", "Waiting".asJson))(obj)
    case obj: ActingPlayer     => deriveEncoder[ActingPlayer].mapJsonObject(_.add("state", "Acting".asJson))(obj)
    case obj: EndOfGamePlayer  => deriveEncoder[EndOfGamePlayer].mapJsonObject(_.add("state", "EndOfGame".asJson))(obj)
    case obj: EndOfMatchPlayer => deriveEncoder[EndOfMatchPlayer].mapJsonObject(_.add("state", "EndOfMatch".asJson))(obj)
  }

  given Decoder[PlayerState] = Decoder.instance { cursor => cursor.downField("state").as[String].flatMap {
    case "SittingOut" => deriveDecoder[SittingOut].tryDecode(cursor)
    case "Waiting"    => deriveDecoder[WaitingPlayer].tryDecode(cursor)
    case "Acting"     => deriveDecoder[ActingPlayer].tryDecode(cursor)
    case "EndOfGame"  => deriveDecoder[EndOfGamePlayer].tryDecode(cursor)
    case "EndOfMatch" => deriveDecoder[EndOfMatchPlayer].tryDecode(cursor)
  }}
