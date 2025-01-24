package scala.xml

import com.raquo.airstream.core.Signal
import com.raquo.ew

import scala.annotation.{implicitNotFound, nowarn}
import scala.collection.immutable.Seq
import scala.collection.{mutable, Iterator}
import scala.scalajs.js

class NodeBuffer extends Seq[Node] {
  private val underlying: mutable.ArrayBuffer[Node] = mutable.ArrayBuffer.empty
  def iterator: Iterator[Node]                      = underlying.iterator
  def apply(i: Int): Node                           = underlying.apply(i)
  def length: Int                                   = underlying.length
  override protected def className: String          = "NodeBuffer"

  import NodeBuffer.{*, given}

  inline def &+(component: Node): Unit = {
    underlying.addOne(component)
  }

  inline def &+[Component: AcceptableNode](component: Component): Unit = {
    &+(summon[AcceptableNode[Component]].asNode(component))
  }

  inline def &+[CC[x] <: IterableOnce[x], Component: AcceptableNode](components: CC[Component]): Unit = {
    components.iterator.foreach(c => {
      &+(summon[AcceptableNode[Component]].asNode(c))
    })
  }

  // 支持 Source[xxx]
  inline def &+[Component: RenderableNode](source: Source[Component]): Unit = {
    &+(L.child <-- source)
  }

  inline def &+[Component <: ChildNodeBase](source: Source[Component]): Unit = {
    &+(L.child <-- source)
  }

  inline def &+[Component: RenderableNode](source: js.Promise[Component]): Unit = {
    val src = Signal
      .fromJsPromise(source)
      .map(opt => opt.map(c => summon[LaminarRenderableNode[Component]].asNode(c)))
    &+(L.child.maybe <-- src)
  }

  // 为了解决string的问题, string被视作Comparable[_]会导致进入该分支,这里强制限定类型范围,但是应该进入
  // com.raquo.laminar.modifiers.RenderableSeq默认的所有类型都在这里了
  type LaminarRenderableSeqType[A] =
    collection.Seq[A] | //
      scala.Array[A] | //
      js.Array[A] | //
      ew.JsArray[A] | //
      ew.JsVector[A] | //
      LaminarSeq[A]

  inline def &+[Collection[x] <: LaminarRenderableSeqType[x]: LaminarRenderableSeq, Component: RenderableNode](
    source: Source[Collection[Component]]): Unit = {
    &+(L.children <-- source)
  }

  inline def &+[Collection[x] <: LaminarRenderableSeqType[x]: LaminarRenderableSeq](
    source: Source[Collection[ChildNodeBase]]): Unit = {
    &+(L.children <-- source)
  }

  @annotation.targetName("sourceOption")
  inline def &+[Component: RenderableNode](source: Source[Option[Component]]): Unit = {
    &+(L.child.maybe <-- source)
  }

  // 清理掉Text为空的节点或者进行trim操作
  @annotation.targetName("trimText")
  inline def &+(inline text: Text): Unit = { MacrosTool.trimOrDropTextNode(text, this) }

  // 忽略空节点, 本来想用inline 直接忽略相关代码
  // 但是从逻辑上, 这样是被允许的 `<div> {println("init this div")} </div>`
  inline def &+(inline o: scala.Null | Unit): Unit = { o }
}

object NodeBuffer {
  import Node.*

  // laminar RenderableNode
  given laminarRenderable[Component: RenderableNode]: LaminarRenderableNode[Component] =
    new LaminarRenderableNode[Component] {
      override def asNode(value: Component): ChildNodeBase = summon[RenderableNode[Component]].asNode(value).value

      override def asNodeSeq(values: LaminarSeq[Component]): LaminarSeq[ChildNodeBase] = values.map(asNode)

      override def asNodeOption(value: Option[Component]): Option[ChildNodeBase] = value.map(asNode)
    }

  // RenderableNode 所有可渲染节点, 应该是一个 ChildNode
  @implicitNotFound("it is not RenderableNode: [${Component}]")
  trait RenderableNode[Component] {
    def asNode(value: Component): ChildNode
  }

  object RenderableNode {
    def AsTextNode[V](): RenderableNode[V] = (value: V) => new TextNode(value.toString)
    type TextTypes = Boolean | Byte | Short | Int | Long | Float | Double | Char | String | java.lang.Number
    given asText[T <: TextTypes]: RenderableNode[T]           = AsTextNode()
    given lmainarChild[T <: ChildNodeBase]: RenderableNode[T] = (v: T) => v
    given elemAsNode: RenderableNode[Elem]                    = (oc: Elem) => oc.reactiveElement

    given unionAll[T <: TextTypes | Elem | ChildNode | ChildNodeBase]: RenderableNode[T] = {
      case e: Elem            => elemAsNode.asNode(e)
      case mod: ChildNodeBase => lmainarChild.asNode(mod)
      case node: ChildNode    => node
      case o                  => new TextNode(o.toString)
    }
  }

  // 所有可以被接受的节点, 不能用于 Source[V]中
  trait AcceptableNode[Component] {
    def asNode(value: Component): Node
  }

  object AcceptableNode {
    given renderable[C: RenderableNode]: AcceptableNode[C] = (c: C) => summon[RenderableNode[C]].asNode(c)
    given laminarMod[C <: LAnyMod]: AcceptableNode[C]      = (v: C) => Node.mod(v)

    import scala.compiletime.*
    import scala.deriving.Mirror

    // support Tuple, 我应该使用推导,还是 UnionType?
    @nowarn
    inline given tupleAsNode[T <: Tuple](using m: Mirror.Of[T]): AcceptableNode[T] = (tuple: T) => {
      val nodeSeqs = toNodeVectors[m.MirroredElemTypes](tuple)
      Node(nodeSeqs)
    }

    private inline def toNodeVectors[Mets](
      p: Product,
      i: Int = 0,
      res: Vector[Node] = Vector.empty,
    ): Vector[Node] = {
      inline erasedValue[Mets] match
        case _: EmptyTuple        => res
        case _: (met *: metsTail) =>
          val acceptable = summonInline[AcceptableNode[met]]
          val node       = acceptable.asNode(p.productElement(i).asInstanceOf[met])
          val nextRes    = res.appended(node)
          toNodeVectors[metsTail](p, i + 1, nextRes)
    }
  }
}
