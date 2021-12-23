package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

val PlayerComponent =
  ScalaComponent
    .builder[Option[PlayerState]]
    .noBackend
    .render_P { seat =>
      val classes = "player" :: (seat match {
        case None => List("empty-seat")
        case Some(SittingOut(_)) => List("sitting-out")
        case Some(WaitingPlayer(_)) => List("sitting-in", "waiting")
        case Some(ActingPlayer(_, _, _)) => List("sitting-in", "acting")
        case Some(EndOfGamePlayer(_, _, true)) => List("sitting-in", "game-winner")
        case Some(EndOfGamePlayer(_, _, false)) => List("sitting-in", "game-loser")
        case Some(EndOfMatchPlayer(_, true)) => List("sitting-in", "match-winner")
        case Some(EndOfMatchPlayer(_, false)) => List("sitting-in", "match-loser")
      })

      <.div(^.className := classes.mkString(" "),
        seat.whenDefined { player => <.div(^.className := "player-name", player.name) },
        seat.collect { case active: SittingIn => active.player.points }.whenDefined { points => <.div(^.className := "match-points", s"$points points") }
      )
    }
    .build