package bastoni.domain.logic.tressette

import bastoni.domain.model.*
import bastoni.domain.model.Rank.*
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.syntax.*

object TressetteGameScoreCalculator:
  def apply(team: List[Player], rete: Boolean): TressetteGameScore =
    TressetteGameScore(
      team.map(_.id),
      Option.when(rete)(TressetteGameScoreItem.Rete).toList ++ team.flatMap(_.taken).collect {
        case card if card.rank == Asso => TressetteGameScoreItem.Carta(card, 3)
        case card if card.rank == Tre => TressetteGameScoreItem.Carta(card, 1)
        case card if card.rank == Due => TressetteGameScoreItem.Carta(card, 1)
        case card if card.rank == Re => TressetteGameScoreItem.Carta(card, 1)
        case card if card.rank == Cavallo => TressetteGameScoreItem.Carta(card, 1)
        case card if card.rank == Fante => TressetteGameScoreItem.Carta(card, 1)
      }
    )
