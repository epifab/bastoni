package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.Palette
import bastoni.frontend.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.VdomElement
import reactkonva.{KArc, KCircle, KGroup, KStar, KText}

object PlayerComponent:
  private val circleStrokeSize: Int = 15

  case class Props(player: PlayerState, layout: SeatLayout, dealer: Boolean)

  private val component =
    ScalaFnComponent[Props] { case Props(playerState: PlayerState, layout: SeatLayout, dealer) =>
      val textRadius: Double = Math.sqrt(2 * layout.radius * layout.radius)
      val textTopLeftAngle: Angle = Angle(225 - layout.textRotation.deg)
      val textPosition: Point = Point(
        layout.center.x + textTopLeftAngle.sin * textRadius,
        layout.center.y + textTopLeftAngle.cos * textRadius
      )

      val timeoutBars = Some(playerState).collect {
        case PlayerState.ActingPlayer(_, _, timeout) =>
          TimeoutBar(
            center = layout.center,
            timeout = timeout,
            angle = Angle(220),
            rotation = layout.rotation,
            innerRadius = layout.radius,
            size = circleStrokeSize
          )
      }

      val stars = Some(playerState).collect {
        case player: PlayerState.SittingIn =>
          PlayerStars(player, layout)
      }

      val dealerFlag = Option.when(dealer)(DealerFlag(layout))

      KGroup(
        KCircle { p =>
          p.radius = layout.radius
          p.x = layout.center.x
          p.y = layout.center.y
          p.fill = Palette.desaturatedBlue

          playerState match {
            case PlayerState.SittingOut(_) =>
              p.opacity = .4

            case PlayerState.EndOfMatchPlayer(_, true) =>
              p.stroke = Palette.yellow1
              p.strokeWidth = 10
              p.shadowColor = Palette.blue
              p.shadowBlur = 30
              p.shadowOpacity = 1

            case PlayerState.EndOfGamePlayer(_, _, true) =>
              p.stroke = Palette.yellow1
              p.strokeWidth = circleStrokeSize
              p.shadowColor = Palette.blue
              p.shadowBlur = circleStrokeSize
              p.shadowOpacity = 1

            case _ => ()
          }
        },

        KText { p =>
          p.text = playerState.name
          p.align = "center"
          p.verticalAlign = "middle"
          p.fill = Palette.white
          p.fontFamily = "'Open Sans', sans-serif"
          p.fontStyle = "bold"
          p.fontSize = 16
          p.wrap = "none"
          p.x = textPosition.x
          p.y = textPosition.y
          p.width = layout.radius * 2
          p.height = layout.radius * 2
          p.rotation = layout.textRotation.deg
        },

        KGroup(timeoutBars.toList ++ dealerFlag.toList ++ stars.toList: _*)
      )
    }

  def apply(state: PlayerState, layout: SeatLayout, dealer: Boolean): VdomElement =
    component
      .withKey(s"player-${state.id}")
      .apply(Props(state, layout, dealer))
