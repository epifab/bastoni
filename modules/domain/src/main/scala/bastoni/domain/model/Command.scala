package bastoni.domain.model

import bastoni.domain.model.Command.PlayCard
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

sealed trait Command

object Command:

  case class Connect(user: User)                                                        extends Command
  case class JoinTable(user: User, seed: Int)                                           extends Command
  case class LeaveTable(user: User)                                                     extends Command
  case class StartMatch(playerId: UserId, gameType: GameType)                           extends Command
  case class ShuffleDeck(seed: Int)                                                     extends Command
  case class PlayCard(playerId: UserId, card: VisibleCard)                              extends Command
  case class TakeCards(playerId: UserId, played: VisibleCard, taken: List[VisibleCard]) extends Command
  case class Ok(playerId: UserId)                                                       extends Command
  case object Continue                                                                  extends Command
  case class Act(playerId: UserId, action: Action, timeout: Option[Timeout.Active])     extends Command
  case class Tick(ref: Int)                                                             extends Command

  given Encoder[Command] = ConfiguredEncoder.derive(discriminator = Some("type"))
  given Decoder[Command] = ConfiguredDecoder.derive(discriminator = Some("type"))
