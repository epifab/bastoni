package bastoni.domain.view

import bastoni.domain.model.{GameType, VisibleCard}
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

sealed trait FromPlayer

object FromPlayer:
  case object Connect                                              extends FromPlayer
  case object JoinRoom                                             extends FromPlayer
  case object LeaveRoom                                            extends FromPlayer
  case class StartMatch(gameType: GameType)                        extends FromPlayer
  case object ShuffleDeck                                          extends FromPlayer
  case object Ok                                                   extends FromPlayer
  case class PlayCard(card: VisibleCard)                           extends FromPlayer
  case class TakeCards(card: VisibleCard, take: List[VisibleCard]) extends FromPlayer
  case object Pong                                                 extends FromPlayer

  given Encoder[FromPlayer] = ConfiguredEncoder.derive(discriminator = Some("messageType"))
  given Decoder[FromPlayer] = ConfiguredDecoder.derive(discriminator = Some("messageType"))
