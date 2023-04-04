package bastoni.frontend.components

import bastoni.domain.model.{RoomPlayerView, User}
import bastoni.domain.view.FromPlayer
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.ScalaComponent

object GameScoreDiv:
  case class State(mouseOver: Boolean)

  private val component = ScalaComponent
    .builder[GameState]
    .stateless
    .noBackend
    .render_P { case GameState(room, _, sendMessage) =>
      val playersScore: Option[List[(String, Int)]] = for
        matchInfo  <- room.matchInfo
        gameScores <- matchInfo.gameScore
        scores = gameScores.map { score =>
          val playerNames = score.playerIds
            .flatMap(playerId => room.players.get(playerId))
            .map(_.name)
            .mkString(", ")
          playerNames -> score.points
        }
      yield scores

      <.div(
        playersScore.whenDefined { scores =>
          TagMod(
            ^.className := "game-score-info",
            <.table(
              scores.map { case (playerNames, score) =>
                <.tr(
                  <.th(<.span(playerNames)),
                  <.td(<.span(s"$score points"))
                )
              }.toTagMod
            ),
            <.div(<.button(^.onClick --> sendMessage(FromPlayer.Ok), "Ok"))
          )
        }
      )
    }
    .build

  def apply(state: GameState): VdomNode = component(state)
end GameScoreDiv
