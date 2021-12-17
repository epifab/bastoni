package bastoni.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

sealed trait PlayerState:
  def playerId: PlayerId

case class SittingOut(player: Player) extends PlayerState:
  val playerId: PlayerId = player.id
  def sitIn: WaitingPlayer = WaitingPlayer(GamePlayer(player, points = 0))

sealed trait SittingIn extends PlayerState:
  def player: GamePlayer
  def playerId: PlayerId = player.id
  def map(f: GamePlayer => GamePlayer): SittingIn
  def sitOut: SittingOut = SittingOut(player.player)
  def act(action: Action): ActingPlayer = ActingPlayer(player, action)

case class WaitingPlayer(player: GamePlayer) extends SittingIn:
  def map(f: GamePlayer => GamePlayer): WaitingPlayer  = copy(player = f(player))

case class ActingPlayer(player: GamePlayer, action: Action) extends SittingIn:
  def map(f: GamePlayer => GamePlayer): ActingPlayer  = copy(player = f(player))
  def done: WaitingPlayer = WaitingPlayer(player)

case class EndOfMatchPlayer(player: GamePlayer, points: Int, winner: Boolean) extends SittingIn:
  def map(f: GamePlayer => GamePlayer): EndOfMatchPlayer  = copy(player = f(player))

case class EndOfGamePlayer(player: GamePlayer, winner: Boolean) extends SittingIn:
  def map(f: GamePlayer => GamePlayer): EndOfGamePlayer = copy(player = f(player))


object PlayerState:

  given Encoder[PlayerState] = Encoder.instance {
    case obj: SittingOut       => deriveEncoder[SittingOut].mapJsonObject(_.add("type", "SittingOut".asJson))(obj)
    case obj: WaitingPlayer     => deriveEncoder[WaitingPlayer].mapJsonObject(_.add("type", "ActivePlayer".asJson))(obj)
    case obj: ActingPlayer     => deriveEncoder[ActingPlayer].mapJsonObject(_.add("type", "ActingPlayer".asJson))(obj)
    case obj: EndOfMatchPlayer => deriveEncoder[EndOfMatchPlayer].mapJsonObject(_.add("type", "EndOfMatchPlayer".asJson))(obj)
    case obj: EndOfGamePlayer  => deriveEncoder[EndOfGamePlayer].mapJsonObject(_.add("type", "EndOfGamePlayer".asJson))(obj)
  }

  given Decoder[PlayerState] = Decoder.instance { cursor => cursor.downField("type").as[String].flatMap {
    case "SittingOut"       => deriveDecoder[SittingOut].tryDecode(cursor)
    case "ActivePlayer"     => deriveDecoder[WaitingPlayer].tryDecode(cursor)
    case "ActingPlayer"     => deriveDecoder[ActingPlayer].tryDecode(cursor)
    case "EndOfMatchPlayer" => deriveDecoder[EndOfMatchPlayer].tryDecode(cursor)
    case "EndOfGamePlayer"  => deriveDecoder[EndOfGamePlayer].tryDecode(cursor)
  }}
