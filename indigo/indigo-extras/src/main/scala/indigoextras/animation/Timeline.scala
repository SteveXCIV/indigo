package indigoextras.animation

import indigo.shared.Lens
import indigo.shared.time.Seconds
import indigo.shared.time.GameTime
import indigo.shared.datatypes.Point
import indigo.shared.datatypes.Radians
import indigo.shared.datatypes.Vector2

import scala.annotation.tailrec

final case class Timeline[A](markers: List[Marker[A]], playhead: Seconds) {

  lazy val sortedMarkers: List[Marker[A]] =
    markers.sortWith((m1, m2) => m1.position.value < m2.position.value)

  lazy val duration: Seconds =
    sortedMarkers.lastOption.map(_.position).getOrElse(Seconds.zero)

  def addMarker(marker: Marker[A]): Timeline[A] =
    this.copy(markers = marker :: markers)

  def play(gameTime: GameTime): Timeline[A] = {
    val next = playhead + gameTime.delta
    this.copy(playhead = if (next >= duration) duration else next)
  }

  def reverse(gameTime: GameTime): Timeline[A] = {
    val next = playhead - gameTime.delta
    this.copy(playhead = if (next < Seconds.zero) Seconds.zero else next)
  }

  def pause: Timeline[A] =
    this

  def stop: Timeline[A] =
    this

  def skipTo(time: Seconds): Timeline[A] =
    this.copy(playhead = if (time < Seconds.zero) Seconds.zero else time)

  def jumpToStart: Timeline[A] =
    this.copy(playhead = Seconds.zero)

  def jumpToEnd: Timeline[A] =
    this.copy(playhead = duration)

  def jumpTo(label: MarkerLabel): Timeline[A] =
    markers.find(_.label == label) match {
      case None =>
        this

      case Some(marker) =>
        this.copy(playhead = marker.position)
    }

  def jumpToNext: Timeline[A] =
    nextMarker.map(jumpTo).getOrElse(this)

  def jumpToPrevious: Timeline[A] =
    previousMarker.map(jumpTo).getOrElse(this)

  def nextMarker: Option[MarkerLabel] = {
    @tailrec
    def rec(remaining: List[Marker[A]]): Option[MarkerLabel] =
      remaining match {
        case Nil =>
          None

        case m :: _ if m.position > playhead =>
          Some(m.label)

        case m :: ms =>
          rec(ms)
      }

    rec(sortedMarkers)
  }

  def previousMarker: Option[MarkerLabel] = {
    @tailrec
    def rec(remaining: List[Marker[A]], last: Option[MarkerLabel]): Option[MarkerLabel] =
      remaining match {
        case Nil =>
          last

        case m :: _ if m.position > playhead =>
          last

        case m :: ms =>
          rec(ms, Some(m.label))
      }

    rec(sortedMarkers, None)
  }

  def progress: Double =
    playhead.value / duration.value

  def tweenProgress: Double = {
    @tailrec
    def rec(remaining: List[Marker[A]], last: Seconds): Double =
      remaining match {
        case Nil =>
          if (last.value != 0)
            (playhead.value - last.value) / (duration.value - last.value)
          else
            playhead.value / duration.value

        case m :: _ if m.position >= playhead =>
          if (last.value != 0)
            (playhead.value - last.value) / (m.position.value - last.value)
          else
            playhead.value / m.position.value

        case m :: ms =>
          rec(ms, m.position)
      }

    rec(sortedMarkers, Seconds.zero)
  }

  // def transformDiff: TransformDiff = {
  //   @tailrec
  //   def rec(remaining: List[Marker], last: Seconds, acc: TransformDiff): TransformDiff =
  //     remaining match {
  //       case Nil =>
  //         val tweenAmount =
  //           if (last.value != 0)
  //             (playhead.value - last.value) / (duration.value - last.value)
  //           else
  //             playhead.value / duration.value

  //         acc

  //       case m :: _ if m.position >= playhead =>
  //         val tweenAmount =
  //           if (last.value != 0)
  //             (playhead.value - last.value) / (m.position.value - last.value)
  //           else
  //             playhead.value / m.position.value

  //         acc.tweenTo(m.diff, tweenAmount)

  //       case m :: ms =>
  //         rec(ms, m.position, acc.chooseLatest(m.diff))
  //     }

  //   rec(sortedMarkers, Seconds.zero, TransformDiff.NoChange)
  // }

}
object Timeline {

  def empty[A]: Timeline[A] =
    Timeline[A](Nil)

  def apply[A](markers: Marker[A]*): Timeline[A] =
    Timeline(markers.toList)
  def apply[A](markers: List[Marker[A]]): Timeline[A] =
    Timeline(markers, Seconds.zero)

}

final case class Marker[A](label: MarkerLabel, position: Seconds, tweens: List[Tween[A, _]])

final case class Tween[A, N](lens: Lens[A, N], to: N)(implicit ev: Numeric[N])
// {

// def moveTo(x: Int, y: Int): TransformDiff =
//   diff.moveTo(x, y)
// def moveTo(newPosition: Point): TransformDiff =
//   diff.moveTo(newPosition)

// def rotateTo(newRotation: Radians): TransformDiff =
//   diff.rotateTo(newRotation)

// def scaleTo(x: Double, y: Double): TransformDiff =
//   diff.scaleTo(x, y)
// def scaleTo(newScale: Vector2): TransformDiff =
//   diff.scaleTo(newScale)

// }
// object Marker {

//   def apply(label: MarkerLabel, position: Seconds): Marker =
//     Marker(label, position, TransformDiff.NoChange)

// }

final case class MarkerLabel(value: String) extends AnyVal

// final case class TransformDiff(maybeMoveTo: Option[Point], maybeRotateTo: Option[Radians], maybeScaleTo: Option[Vector2]) {

//   def chooseLatest(next: TransformDiff): TransformDiff =
//     TransformDiff(
//       (maybeMoveTo, next.maybeMoveTo) match {
//         case (None, None) => None
//         case (p, None)    => p
//         case (None, p)    => p
//         case (Some(_), p) => p
//       },
//       (maybeRotateTo, next.maybeRotateTo) match {
//         case (None, None) => None
//         case (p, None)    => p
//         case (None, p)    => p
//         case (Some(_), p) => p
//       },
//       (maybeScaleTo, next.maybeScaleTo) match {
//         case (None, None) => None
//         case (p, None)    => p
//         case (None, p)    => p
//         case (Some(_), p) => p
//       }
//     )

//   def tweenTo(next: TransformDiff, amount: Double): TransformDiff =
//     TransformDiff(
//       (maybeMoveTo, next.maybeMoveTo) match {
//         case (None, None) =>
//           None

//         case (p, None) =>
//           p

//         case (None, Some(p)) =>
//           Some(
//             Point(
//               x = (p.x.toDouble * amount).toInt,
//               y = (p.y.toDouble * amount).toInt
//             )
//           )

//         case (Some(p1), Some(p2)) =>
//           Some(
//             Point(
//               x = ((p2.x.toDouble - p1.x.toDouble) * amount).toInt,
//               y = ((p2.y.toDouble - p1.y.toDouble) * amount).toInt
//             )
//           )
//       },
//       (maybeRotateTo, next.maybeRotateTo) match {
//         case (None, None) =>
//           None

//         case (p, None) =>
//           p

//         case (None, Some(p)) =>
//           Some(Radians(p.value * amount))

//         case (Some(p1), Some(p2)) =>
//           Some(Radians((p2.value - p1.value) * amount))
//       },
//       (maybeScaleTo, next.maybeScaleTo) match {
//         case (None, None) =>
//           None

//         case (p, None) =>
//           p

//         case (None, Some(p)) =>
//           Some(
//             Vector2(
//               x = p.x * amount,
//               y = p.y * amount
//             )
//           )

//         case (Some(p1), Some(p2)) =>
//           Some(
//             Vector2(
//               x = (p2.x - p1.x) * amount,
//               y = (p2.y - p1.y) * amount
//             )
//           )
//       }
//     )

//   def moveTo(x: Int, y: Int): TransformDiff =
//     moveTo(Point(x, y))
//   def moveTo(newPosition: Point): TransformDiff =
//     this.copy(maybeMoveTo = Option(newPosition))

//   def rotateTo(newRotation: Radians): TransformDiff =
//     this.copy(maybeRotateTo = Option(newRotation))

//   def scaleTo(x: Double, y: Double): TransformDiff =
//     scaleTo(Vector2(x, y))
//   def scaleTo(newScale: Vector2): TransformDiff =
//     this.copy(maybeScaleTo = Option(newScale))

// }
// object TransformDiff {

//   val NoChange: TransformDiff =
//     TransformDiff(None, None, None)

// }