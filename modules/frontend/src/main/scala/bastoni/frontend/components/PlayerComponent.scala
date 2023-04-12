package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import reactkonva.{KArc, KCircle, KGroup, KStar, KText}

object PlayerComponent:
  private val circleStrokeSize: Int = 15

  case class Props(player: PlayerState, layout: SeatLayout, dealer: Boolean)

  private val component =
    ScalaFnComponent[Props] { case Props(playerState: PlayerState, layout: SeatLayout, dealer) =>
      val textRadius: Double      = Math.sqrt(2 * layout.radius * layout.radius)
      val textTopLeftAngle: Angle = Angle(225 - layout.textRotation.deg)
      val textPosition: Point = Point(
        layout.center.x + textTopLeftAngle.sin * textRadius,
        layout.center.y + textTopLeftAngle.cos * textRadius
      )

      val timedOut = playerState match
        case PlayerState.Acting(_, _, Some(Timeout.TimedOut)) => true
        case _                                                => false

      val timeoutBars: Option[VdomNode] = Some(playerState)
        .collect {
          case PlayerState.Acting(_, _, Some(timeout: Timeout.Active)) => Some(timeout)
          case PlayerState.Acting(_, _, None)                          => None
        }
        .map { timeout =>
          TimeoutBar(
            center = layout.center,
            timeout = timeout,
            angle = Angle(220),
            rotation = layout.rotation,
            innerRadius = layout.radius,
            size = circleStrokeSize
          )
        }

      val stars: Option[VdomNode] = Some(playerState).collect { case player: PlayerState.SittingIn =>
        PlayerStars(player, layout)
      }

      val dealerFlag: Option[VdomNode] = Option.when(dealer)(DealerFlag(layout))

      KGroup(
        { group => if (timedOut) group.opacity = .5 },
        KCircle { circle =>
          circle.radius = layout.radius
          circle.x = layout.center.x
          circle.y = layout.center.y
          circle.fill = Palette.desaturatedBlue

          playerState match
            case PlayerState.SittingOut(_) =>
              circle.opacity = .4

            case PlayerState.EndOfMatch(_, true) =>
              circle.stroke = Palette.yellow1
              circle.strokeWidth = 10
              circle.shadowColor = Palette.blue
              circle.shadowBlur = 30
              circle.shadowOpacity = 1

            case PlayerState.EndOfGame(_, _, true) =>
              circle.stroke = Palette.yellow1
              circle.strokeWidth = circleStrokeSize
              circle.shadowColor = Palette.blue
              circle.shadowBlur = circleStrokeSize
              circle.shadowOpacity = 1

            case _ => ()
        },
        KText { text =>
          text.text = playerState.name
          text.align = "center"
          text.verticalAlign = "middle"
          text.fill = Palette.white
          text.fontFamily = "'Open Sans', sans-serif"
          text.fontStyle = "bold"
          text.fontSize = 16
          text.wrap = "none"
          text.x = textPosition.x
          text.y = textPosition.y
          text.width = layout.radius * 2
          text.height = layout.radius * 2
          text.rotation = layout.textRotation.deg
        },
        KGroup(timeoutBars.toList ++ dealerFlag.toList ++ stars.toList: _*)
      )
    }

  def apply(state: PlayerState, layout: SeatLayout, dealer: Boolean): VdomElement =
    component
      .withKey(s"player-${state.id}")
      .apply(Props(state, layout, dealer))
end PlayerComponent
