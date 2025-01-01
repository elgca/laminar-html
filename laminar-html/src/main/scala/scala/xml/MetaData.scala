package scala.xml

import com.raquo.airstream.core.Source
import com.raquo.laminar.modifiers.{EventListener, Modifier}
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom

import scala.annotation.{implicitNotFound, tailrec}

trait MetaData {

  def foreach(f: MetaData => Unit): Unit = {
    @tailrec
    def foreach0(m: MetaData): Unit =
      m match {
        case scala.xml.Null =>
        case any            => f(any); foreach0(m.next)
      }

    foreach0(this)
  }

  def next: MetaData

  def apply(namespaceBinding: NamespaceBinding, element: Elem): Unit = {}

}

case object Null extends MetaData {
  override def next: MetaData = this
}

class UnprefixedAttribute[V: AttributeBinder](
  val key: String,
  val value: V,
  val next: MetaData,
) extends MetaData {

  override def apply(namespaceBinding: NamespaceBinding, element: Elem): Unit = {
    summon[AttributeBinder[V]].bindAttr(element, None, key, value)
  }
}

class PrefixedAttribute[V: AttributeBinder](
  val prefix: String,
  val key: String,
  val value: V,
  val next: MetaData,
) extends MetaData {

  override def apply(namespaceBinding: NamespaceBinding, element: Elem): Unit = {
    summon[AttributeBinder[V]].bindAttr(
      element,
      namespaceBinding.namespaceURI(prefix),
      s"$prefix:$key",
      value,
    )
  }
}

@implicitNotFound(msg = """无法在 HTML 属性中嵌入类型为 ${T} 的值，未找到隐式的 AttributeBinder[${T}]。
  以下类型是支持的：
  - String
  - Boolean（false → 移除属性，true → 空属性）
  - Source[T]:Var[T],Signal[T]等
  - Option[String,Boolean]
  """)
trait AttributeBinder[T] {
  def bindAttr(element: Elem, namespaceURI: Option[String], key: String, value: T): Unit
}

object AttributeBinder {

  // 基础数据类型
  @implicitNotFound(msg = """无法在 XML 属性中嵌入基础类型 ${T}，未找到隐式的 AttrValue[${T}]。
  以下类型是支持的：
  - String
  - Boolean: false → 移除属性; true → 空属性
  - Int/Double: _.toString
  - List[String]: _.mkString(" ")
  - Option[T]
  """)
  trait AttrValue[T] extends ((dom.Element, Option[String], String, T) => Unit)

  object AttrValue {

    def removeAttr(node: dom.Element, ns: Option[String], key: String): Unit =
      ns.fold(node.removeAttribute(key))(ns => node.removeAttributeNS(ns, key))

    given SetAttr: AttrValue[String] = (node, ns, key, value) =>
      ns.fold(node.setAttribute(key, value))(ns => node.setAttributeNS(ns, key, value))

    given BoolSetter: AttrValue[Boolean] = (node, ns, key, value) =>
      if value then SetAttr(node, ns, key, "") else removeAttr(node, ns, key)

    given DoubleSetter: AttrValue[Double] = (node, ns, key, value) => SetAttr(node, ns, key, value.toString)

    given IntSetter: AttrValue[Int] = (node, ns, key, value) => SetAttr(node, ns, key, value.toString)

    given TextSetter: AttrValue[Text] = (node, ns, key, value) => SetAttr(node, ns, key, value.data)

    given OptionSetter[T, OPT[x] <: Option[x]](using setter: AttrValue[T]): AttrValue[OPT[T]] =
      (node, ns, key, value) =>
        value match {
          case None        => removeAttr(node, ns, key)
          case Some(value) => setter(node, ns, key, value)
        }

    given NilSetter: AttrValue[Nil.type] = (node, ns, key, value) => SetAttr(node, ns, key, "")

    given ListSetter[SEQ[x] <: Seq[x]]: AttrValue[SEQ[String]] = (node, ns, key, value) =>
      SetAttr(node, ns, key, value.map(_.trim).filter(_.isEmpty).mkString(" "))
  }

  given PrimaryBinder[T](using setter: AttrValue[T]): AttributeBinder[T] = { (element, namespaceURI, key, value) =>
    setter(element.ref, namespaceURI, key, value)
  }

  given SourceBinder[T, S[x] <: Source[x]](using setter: AttrValue[T]): AttributeBinder[S[T]] = {
    (element, namespaceURI, key, value) =>
      ReactiveElement.bindFn(element, value.toObservable) { nextValue =>
        setter(element.ref, namespaceURI, key, nextValue)
      }
  }

  given Fun0Binder: AttributeBinder[() => Unit] = { (element, namespaceURI, key, fun) =>
    element.ref.addEventListener(key, (_: dom.Event) => fun())
  }

  given Fun1Binder[Event <: dom.Event]: AttributeBinder[Event => Unit] = { (element, namespaceURI, key, fun) =>
    element.ref.addEventListener(key, (ev: Event) => fun(ev))
  }

  given EventListenerBinder[Ev <: dom.Event, Out]: AttributeBinder[EventListener[Ev, Out]] = {
    (element, namespaceURI, key, listener) =>
      listener.apply(element)
  }

  given htmlModBinder[Ref <: Modifier.Base]: AttributeBinder[Ref] = { (element, namespaceURI, key, mod) =>
    mod.apply(element)
  }
}
