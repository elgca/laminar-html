package scala.xml

import scala.collection.Iterable

transparent trait Node extends Any {
  def apply(element: ReactiveElementBase): Unit
}

object Node {

  trait ModifierNode extends Any with Node

  class ChildNode(val value: ChildNodeBase) extends AnyVal with Node {
    override def apply(element: ReactiveElementBase): Unit = value(element)
  }

  implicit inline def toNode[T <: ChildNodeBase](inline any: T): ChildNode = new ChildNode(any)

  class LamianrModifierNode(val value: LModBase) extends AnyVal with ModifierNode {
    override def apply(element: ReactiveElementBase): Unit = value(element)
  }

  class GroupNode(val group: Iterable[Node]) extends AnyVal with ModifierNode {
    override def apply(element: ReactiveElementBase): Unit = group.iterator.foreach(_.apply(element))
  }

  def apply[CC[x] <: Iterable[x]](group: CC[Node]): ModifierNode = new GroupNode(group)

  def mod[T <: LAnyMod](mod: T): ModifierNode = { LamianrModifierNode(mod.asInstanceOf[LModBase]) }
}
