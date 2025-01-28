package xhtml
package interpolator

import scala.util.parsing.input.{OffsetPosition, Position}

given Conversion[Position, Int] with
  def apply(pos: Position): Int =
    pos match
      case OffsetPosition(_, offset) => offset
      case _ => throw new Exception(s"expected offset position:$pos")

private[xhtml] inline def ctx(using XmlContext): XmlContext = summon
private[xhtml] inline def reporter(using Reporter): Reporter = summon