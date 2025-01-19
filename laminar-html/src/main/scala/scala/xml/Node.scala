package scala.xml

import com.raquo.laminar.keys.Key
import com.raquo.laminar.modifiers.Modifier
import com.raquo.laminar.nodes.ReactiveElement
import com.raquo.laminar.tags.Tag
import org.scalajs.dom
import org.scalajs.dom.Element

type Node    = Modifier.Base
type AnyNode = Modifier[? <: ReactiveElement.Base]

type Comment  = com.raquo.laminar.nodes.CommentNode
type TextNode = com.raquo.laminar.nodes.TextNode

type ReactiveElementBase = com.raquo.laminar.nodes.ReactiveElement.Base
type ReactiveHtmlElement = com.raquo.laminar.nodes.ReactiveHtmlElement[dom.html.Element]
type ReactiveSvgElement  = com.raquo.laminar.nodes.ReactiveSvgElement[dom.svg.Element]

type RenderableNode[-Component] = com.raquo.laminar.modifiers.RenderableNode[Component]
val RenderableNode = com.raquo.laminar.modifiers.RenderableNode
type RenderableSeq[-Collection[_]] = com.raquo.laminar.modifiers.RenderableSeq[Collection]
val DomApi          = com.raquo.laminar.DomApi
val ReactiveElement = com.raquo.laminar.nodes.ReactiveElement
val L               = com.raquo.laminar.api.L
type Source[+A] = com.raquo.airstream.core.Source[A]

class ElementNodeBase(
  override val tag: Tag[ReactiveElement[Element]],
  override val ref: Element,
) extends ReactiveElementBase {
  override def onBoundKeyUpdater(key: Key): Unit = ()
}
