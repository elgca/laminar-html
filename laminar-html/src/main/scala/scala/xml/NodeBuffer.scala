package scala.xml

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

  // 忽略空节点
  def &+(o: scala.Null | Unit): NodeBuffer = { this }

  def &+(o: AnyNode): NodeBuffer = {
    underlying.addOne(o.asInstanceOf[Node])
    this
  }
}
