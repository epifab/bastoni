package bastoni.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

sealed trait PlayerState:
  def player: Player
  def playerId: PlayerId = player.id

case class SittingOut(override val player: Player) extends PlayerState:
  def sitIn: WaitingPlayer = WaitingPlayer(GamePlayer(player, points = 0))

sealed trait SittingIn(val gamePlayer: GamePlayer) extends PlayerState:
  override val player: Player = gamePlayer.player
  def mapPlayer(f: GamePlayer => GamePlayer): SittingIn
  def sitOut: SittingOut = SittingOut(gamePlayer.player)
  def act(action: Action): ActingPlayer = ActingPlayer(gamePlayer, action)

case class WaitingPlayer(override val gamePlayer: GamePlayer) extends SittingIn(gamePlayer):
  def mapPlayer(f: GamePlayer => GamePlayer): WaitingPlayer  = copy(gamePlayer = f(gamePlayer))

case class ActingPlayer(override val gamePlayer: GamePlayer, action: Action) extends SittingIn(gamePlayer):
  def mapPlayer(f: GamePlayer => GamePlayer): ActingPlayer  = copy(gamePlayer = f(gamePlayer))
  def done: WaitingPlayer = WaitingPlayer(gamePlayer)

case class EndOfMatchPlayer(override val gamePlayer: GamePlayer, points: Int, winner: Boolean) extends SittingIn(gamePlayer):
  def mapPlayer(f: GamePlayer => GamePlayer): EndOfMatchPlayer  = copy(gamePlayer = f(gamePlayer))

case class EndOfGamePlayer(override val gamePlayer: GamePlayer, winner: Boolean) extends SittingIn(gamePlayer):
  def mapPlayer(f: GamePlayer => GamePlayer): EndOfGamePlayer = copy(gamePlayer = f(gamePlayer))


object PlayerState:

  given Encoder[PlayerState] = Encoder.instance {
    case obj: SittingOut       => deriveEncoder[SittingOut].mapJsonObject(_.add("type", "SittingOut".asJson))(obj)
    case obj: WaitingPlayer    => deriveEncoder[WaitingPlayer].mapJsonObject(_.add("type", "WaitingPlayer".asJson))(obj)
    case obj: ActingPlayer     => deriveEncoder[ActingPlayer].mapJsonObject(_.add("type", "ActingPlayer".asJson))(obj)
    case obj: EndOfMatchPlayer => deriveEncoder[EndOfMatchPlayer].mapJsonObject(_.add("type", "EndOfMatchPlayer".asJson))(obj)
    case obj: EndOfGamePlayer  => deriveEncoder[EndOfGamePlayer].mapJsonObject(_.add("type", "EndOfGamePlayer".asJson))(obj)
  }

  given Decoder[PlayerState] = Decoder.instance { cursor => cursor.downField("type").as[String].flatMap {
    case "SittingOut"       => deriveDecoder[SittingOut].tryDecode(cursor)
    case "WaitingPlayer"    => deriveDecoder[WaitingPlayer].tryDecode(cursor)
    case "ActingPlayer"     => deriveDecoder[ActingPlayer].tryDecode(cursor)
    case "EndOfMatchPlayer" => deriveDecoder[EndOfMatchPlayer].tryDecode(cursor)
    case "EndOfGamePlayer"  => deriveDecoder[EndOfGamePlayer].tryDecode(cursor)
  }}
