package bastoni.frontend.components

import bastoni.domain.model.PlayerState
import bastoni.frontend.model.{Angle, Palette, SeatLayout}
import japgolly.scalajs.react.component.ScalaFn
import japgolly.scalajs.react.vdom.VdomNode
import reactkonva.{KGroup, KStar}

object PlayerStars:
  private val component = ScalaFn[(PlayerState.Playing, SeatLayout)] { case (player, layout) =>
    val points      = player.player.points
    val innerRadius = 7
    val outerRadius = 10
    val starSize    = 2 * outerRadius

    val xangle  = Angle(90 - layout.rotation.deg)
    val xoffset = -(starSize * (points - 1) / 2)

    KGroup(
      (0 until points).map { index =>
        KStar { p =>
          p.numPoints = 5
          p.innerRadius = 7
          p.outerRadius = 10
          p.fill = Palette.yellow2
          p.shadowBlur = 4
          p.shadowColor = Palette.grey2
          p.x = layout.center.x + layout.rotation.sin * 30 + xangle.sin * (xoffset + starSize * index)
          p.y = layout.center.y + layout.rotation.cos * 30 + xangle.cos * (xoffset + starSize * index)
        }
      }: _*
    )
  }

  def apply(player: PlayerState.Playing, layout: SeatLayout): VdomNode = component(player -> layout)
