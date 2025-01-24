package scala.xml

import com.raquo.laminar.keys.Key
import com.raquo.laminar.lifecycle
import com.raquo.laminar.modifiers.Modifier
import com.raquo.laminar.nodes.{ChildNode, ReactiveElement}
import com.raquo.laminar.tags.Tag
import org.scalajs.dom
import org.scalajs.dom.Element

type LModBase = Modifier.Base
type LAnyMod  = Modifier[? <: ReactiveElement.Base]

type Comment  = com.raquo.laminar.nodes.CommentNode
type TextNode = com.raquo.laminar.nodes.TextNode

type ReactiveElementBase = com.raquo.laminar.nodes.ReactiveElement.Base
type ReactiveHtmlElement = com.raquo.laminar.nodes.ReactiveHtmlElement[dom.html.Element]
type ReactiveSvgElement  = com.raquo.laminar.nodes.ReactiveSvgElement[dom.svg.Element]

type MountContext[+El <: ReactiveElementBase] = lifecycle.MountContext[El]

type HtmlTag[+El <: dom.html.Element] = com.raquo.laminar.tags.HtmlTag[El]
type SvgTag[+El <: dom.svg.Element]   = com.raquo.laminar.tags.SvgTag[El]
type Tag[+El <: ReactiveElementBase]  = com.raquo.laminar.tags.Tag[El]

type LaminarRenderableNode[-Component] = com.raquo.laminar.modifiers.RenderableNode[Component]
val LaminarRenderableNode = com.raquo.laminar.modifiers.RenderableNode

type LaminarSeq[+A] = com.raquo.laminar.Seq[A]
val LaminarSeq = com.raquo.laminar.Seq

type LaminarRenderableSeq[-Collection[_]] = com.raquo.laminar.modifiers.RenderableSeq[Collection]

val DomApi          = com.raquo.laminar.DomApi
val ReactiveElement = com.raquo.laminar.nodes.ReactiveElement
val L               = com.raquo.laminar.api.L
type Source[+A]    = com.raquo.airstream.core.Source[A]
type ChildNodeBase = ChildNode.Base
type Subscription  = com.raquo.airstream.ownership.Subscription
