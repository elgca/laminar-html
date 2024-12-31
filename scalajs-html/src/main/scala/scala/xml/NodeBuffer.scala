package scala.xml

import scala.collection.immutable.Seq
import scala.collection.{Iterator, mutable}

class NodeBuffer extends Seq[Node] {
  private val underlying: mutable.ArrayBuffer[Node] = mutable.ArrayBuffer.empty
  def iterator: Iterator[Node]                      = underlying.iterator
  def apply(i: Int): Node                           = underlying.apply(i)
  def length: Int                                   = underlying.length
  override protected def className: String          = "NodeBuffer"

  def &+(o: String): NodeBuffer  = { this }
  def &+(o: Int): NodeBuffer     = { this }
  def &+(o: Double): NodeBuffer  = { this }
  def &+(o: Boolean): NodeBuffer = { this }

  // 清理掉Text为空的节点或者进行trim操作
  @annotation.targetName("trimText")
  inline def &+(inline text: Text): NodeBuffer = { TextClean.trimOrDropTextNode(text, this) }

  // 忽略空节点
  def &+(o: Null | Unit): NodeBuffer = { this }

  def &+(o: Node): NodeBuffer = {
    underlying.addOne(o)
    this
  }
}
