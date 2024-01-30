package bastoni.domain.view

import bastoni.domain.model.{GameType, User, VisibleCard}
import cats.Show
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

sealed trait FromPlayer

object FromPlayer:
  sealed trait AuthCommand                   extends FromPlayer
  case class Authenticate(authToken: String) extends AuthCommand

  sealed trait GameCommand                                         extends FromPlayer
  case object Connect                                              extends GameCommand
  case object JoinTable                                            extends GameCommand
  case object LeaveTable                                           extends GameCommand
  case class StartMatch(gameType: GameType)                        extends GameCommand
  case object ShuffleDeck                                          extends GameCommand
  case object Ok                                                   extends GameCommand
  case class PlayCard(card: VisibleCard)                           extends GameCommand
  case class TakeCards(card: VisibleCard, take: List[VisibleCard]) extends GameCommand

  case object Pong extends FromPlayer

  given Encoder[FromPlayer] = ConfiguredEncoder.derive(discriminator = Some("messageType"))
  given Decoder[FromPlayer] = ConfiguredDecoder.derive(discriminator = Some("messageType"))

  given Show[GameCommand] = Show(Encoder[FromPlayer].apply(_).spaces2)
