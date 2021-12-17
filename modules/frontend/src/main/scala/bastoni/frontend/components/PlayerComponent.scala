package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

val PlayerComponent =
  ScalaComponent
    .builder[Option[PlayerState]]
    .noBackend
    .render_P {
      case None => <.div(^.className := "player empty-seat")
      case Some(SittingOut(player)) => <.div(^.className := s"player sitting-out", player.name)
      case Some(WaitingPlayer(MatchPlayer(player, points, dealer))) =>
        <.div(^.className := (List("player", "waiting") ++ Option.when(dealer)("dealer").toList).mkString(" "),
          <.strong(player.name),
          s" ($points points)"
        )
      case Some(ActingPlayer(MatchPlayer(player, points, dealer), _, _)) =>
        <.div(^.className := (List("player", "acting") ++ Option.when(dealer)("dealer").toList).mkString(" "),
          <.strong(player.name),
          s" ($points points)"
        )
      case Some(EndOfGamePlayer(MatchPlayer(player, matchPoints, dealer), points, winner)) =>
        <.div(^.className := (List("player", "end-of-game") ++ Option.when(dealer)("dealer").toList ++ Option.when(winner)("winner")).mkString(" "),
          <.strong(player.name),
          s" ($matchPoints points ($points))"
        )
      case Some(EndOfMatchPlayer(MatchPlayer(player, matchPoints, dealer), winner)) =>
        <.div(^.className := (List("player", "end-of-match") ++ Option.when(dealer)("dealer").toList ++ Option.when(winner)("winner")).mkString(" "),
          <.strong(player.name),
          s" ($matchPoints points)"
        )
    }
    .build