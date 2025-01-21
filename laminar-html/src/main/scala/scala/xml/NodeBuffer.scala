package scala.xml

import com.raquo.laminar
import com.raquo.laminar.modifiers.RenderableNode

import scala.annotation.implicitNotFound
import scala.collection.immutable.Seq
import scala.collection.{mutable, Iterator}

class NodeBuffer extends Seq[Node] {
  private val underlying: mutable.ArrayBuffer[Node] = mutable.ArrayBuffer.empty
  def iterator: Iterator[Node]                      = underlying.iterator
  def apply(i: Int): Node                           = underlying.apply(i)
  def length: Int                                   = underlying.length
  override protected def className: String          = "NodeBuffer"

  import NodeBuffer.*

  def &+[Component: AcceptableNode](component: Component): NodeBuffer = {
    underlying.addOne(summon[AcceptableNode[Component]].asNode(component))
    this
  }

  def &+[CC[x] <: IterableOnce[x], Component: AcceptableNode](components: CC[Component]): NodeBuffer = {
    components.iterator.foreach(c => {
      underlying.addOne(summon[AcceptableNode[Component]].asNode(c))
    })
    this
  }

  // 清理掉Text为空的节点或者进行trim操作
  @annotation.targetName("trimText")
  inline def &+(inline text: Text): NodeBuffer = { MacrosTool.trimOrDropTextNode(text, this) }

  // 忽略空节点
  def &+(o: scala.Null | Unit): NodeBuffer = { this }

//  // 兼容Laminar，我想把Elem继承ReactiveElementBase,但是失败了,无法完全应用Laminar属性
//  // 只有 ReactiveSvgElement 和 ReactiveHtmlElement 可以完全使用Lamianr的Modify
//  def &+(o: Elem): NodeBuffer = {
//    underlying.addOne(o.reactiveElement)
//    this
//  }

//  def &+(o: AnyNode): NodeBuffer = {
//    underlying.addOne(o.asInstanceOf[Node])
//    this
//  }
}

object NodeBuffer {

  @implicitNotFound("")
  trait AcceptableNode[Component] {
    def asNode(value: Component): Node
  }

  object AcceptableNode {
    def AsTextNode[V](): AcceptableNode[V] = (value: V) => new TextNode(value.toString)
    type TextTypes = Boolean | Byte | Short | Int | Long | Float | Double | Char | String | java.lang.Number
    given asText[T <: TextTypes]: AcceptableNode[T]  = AsTextNode()
    given asAnyNode[T <: LAnyMod]: AcceptableNode[T] = (v: T) => v.asInstanceOf[LModBase]
    given elemAsNode: AcceptableNode[Elem]           = (oc: Elem) => oc.reactiveElement

  }
}

object RenderableNodeImplicit {
  given int: RenderableNode[Int]         = RenderableNode(x => new TextNode(x.toString))
  given string: RenderableNode[String]   = RenderableNode(x => new TextNode(x.toString))
  given double: RenderableNode[Double]   = RenderableNode(x => new TextNode(x.toString))
  given boolean: RenderableNode[Boolean] = RenderableNode(x => new TextNode(x.toString))

  trait Node
}
