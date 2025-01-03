package scala.xml

import com.raquo.airstream.core.Source
import scala.collection.immutable.Seq
import scala.collection.{mutable, Iterator}

class NodeBuffer extends Seq[Node] {
  private val underlying: mutable.ArrayBuffer[Node] = mutable.ArrayBuffer.empty
  def iterator: Iterator[Node]                      = underlying.iterator
  def apply(i: Int): Node                           = underlying.apply(i)
  def length: Int                                   = underlying.length
  override protected def className: String          = "NodeBuffer"

  def &+(o: String): NodeBuffer     = { &+(new TextNode(o)) }
  def &+(o: Int): NodeBuffer        = { &+(new TextNode(o.toString)) }
  def &+(o: Long): NodeBuffer       = { &+(new TextNode(o.toString)) }
  def &+(o: BigInt): NodeBuffer     = { &+(new TextNode(o.toString)) }
  def &+(o: BigDecimal): NodeBuffer = { &+(new TextNode(o.toString)) }
  def &+(o: Double): NodeBuffer     = { &+(new TextNode(o.toString)) }
  def &+(o: Boolean): NodeBuffer    = { &+(new TextNode(o.toString)) }

  // 清理掉Text为空的节点或者进行trim操作
  @annotation.targetName("trimText")
  inline def &+(inline text: Text): NodeBuffer = { MacrosTool.trimOrDropTextNode(text, this) }

  @annotation.targetName("addChild")
  def &+[Component: RenderableNode](source: Source[Component]): NodeBuffer = {
    &+(L.child <-- source)
  }

  @annotation.targetName("addChildren")
  def &+[Collection[_]: RenderableSeq, Component: RenderableNode](
    source: Source[Collection[Component]],
  ): NodeBuffer = {
    &+(L.children <-- source)
  }

  // 忽略空节点
  def &+(o: scala.Null | Unit): NodeBuffer = { this }

  // 兼容Laminar，我想把Elem继承ReactiveElementBase,但是失败了,无法完全应用Laminar属性
  def &+(o: Elem): NodeBuffer = {
    underlying.addOne(o.reactiveElement)
    this
  }

  def &+(o: AnyNode): NodeBuffer = {
    underlying.addOne(o.asInstanceOf[Node])
    this
  }
}

object RenderableNodeImplicit {
  given int: RenderableNode[Int]         = RenderableNode(x => new TextNode(x.toString))
  given string: RenderableNode[String]   = RenderableNode(x => new TextNode(x.toString))
  given double: RenderableNode[Double]   = RenderableNode(x => new TextNode(x.toString))
  given boolean: RenderableNode[Boolean] = RenderableNode(x => new TextNode(x.toString))
}
