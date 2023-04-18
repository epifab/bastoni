package bastoni.domain.view

import bastoni.domain.model.{GameType, User, VisibleCard}
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

sealed trait FromPlayer

object FromPlayer:
  case class Authenticate(authToken: String)                       extends FromPlayer
  case object Connect                                              extends FromPlayer
  case object JoinTable                                            extends FromPlayer
  case object LeaveTable                                           extends FromPlayer
  case class StartMatch(gameType: GameType)                        extends FromPlayer
  case object ShuffleDeck                                          extends FromPlayer
  case object Ok                                                   extends FromPlayer
  case class PlayCard(card: VisibleCard)                           extends FromPlayer
  case class TakeCards(card: VisibleCard, take: List[VisibleCard]) extends FromPlayer
  case object Pong                                                 extends FromPlayer

  given Encoder[FromPlayer] = ConfiguredEncoder.derive(discriminator = Some("messageType"))
  given Decoder[FromPlayer] = ConfiguredDecoder.derive(discriminator = Some("messageType"))
