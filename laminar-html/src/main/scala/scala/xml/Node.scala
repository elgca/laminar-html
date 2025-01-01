package scala.xml

import com.raquo.laminar.keys.Key
import com.raquo.laminar.modifiers.Modifier
import com.raquo.laminar.nodes.ReactiveElement

type Node    = Modifier.Base
type AnyNode = Modifier[? <: ReactiveElement.Base]

type Comment  = com.raquo.laminar.nodes.CommentNode
type TextNode = com.raquo.laminar.nodes.TextNode

type ReactiveElementBase = com.raquo.laminar.nodes.ReactiveElement.Base

trait ElementNodeBase extends ReactiveElementBase {
  override def onBoundKeyUpdater(key: Key): Unit = ()
}
