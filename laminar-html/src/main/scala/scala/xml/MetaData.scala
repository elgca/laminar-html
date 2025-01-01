package scala.xml

import com.raquo.airstream.core.Source
import org.scalajs.dom

import scala.annotation.{implicitNotFound, tailrec}
import scala.collection.immutable.{List, Nil}
import scala.scalajs.js

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

  def apply(namespaceBinding: NamespaceBinding, element: ReactiveElementBase): Unit = {}

}

case object Null extends MetaData {
  override def next: MetaData = this
}

class UnprefixedAttribute[V: AttributeBinder](
  val key: String,
  val value: V,
  val next: MetaData,
) extends MetaData {

  override def apply(namespaceBinding: NamespaceBinding, element: ReactiveElementBase): Unit = {
    summon[AttributeBinder[V]].bindAttr(element, None, key, value)
  }
}

class PrefixedAttribute[V: AttributeBinder](
  val prefix: String,
  val key: String,
  val value: V,
  val next: MetaData,
) extends MetaData {

  override def apply(namespaceBinding: NamespaceBinding, element: ReactiveElementBase): Unit = {
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
  def bindAttr(element: ReactiveElementBase, namespaceURI: Option[String], key: String, value: T): Unit
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

    def removeAttr(node: dom.Element, ns: Option[String], key: String): Unit = {
      ns.fold(node.removeAttribute(key))(ns => node.removeAttributeNS(ns, key))
    }

    def setAttribute(node: dom.Element, ns: Option[String], key: String, value: String): Unit = {
      ns.fold {
        if Props.props.contains(key) then setProperty(node, ns, key, value)
        else node.setAttribute(key, value)
      } { ns =>
        node.setAttributeNS(ns, key, value)
      }
    }

    def setProperty[DomV](node: dom.Element, ns: Option[String], key: String, value: DomV): Unit = {
      node.asInstanceOf[js.Dynamic].updateDynamic(key)(value.asInstanceOf[js.Any])
    }

    def getProperty[DomV](node: dom.Element, key: String): js.UndefOr[DomV] = {
      node.asInstanceOf[js.Dynamic].selectDynamic(key).asInstanceOf[js.UndefOr[DomV]]
    }

    given SetAttr: AttrValue[String] = (node, ns, key, value) => setAttribute(node, ns, key, value)

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
      val updater: (dom.Element, Option[String], String, T) => Unit =
        if key == "value" && namespaceURI.isEmpty then
          (e: dom.Element, ns: Option[String], key: String, nextValue: T) =>
            if !AttrValue.getProperty(e, key).contains(nextValue) then AttrValue.setProperty(e, ns, key, nextValue)
        else if namespaceURI.isEmpty && Props.props.contains(key) then AttrValue.setProperty
        else setter
      ReactiveElement.bindFn(element, value.toObservable) { nextValue =>
        updater(element.ref, namespaceURI, key, nextValue)
      }
  }

  given Fun0Binder: AttributeBinder[() => Unit] = { (element, namespaceURI, key, fun) =>
    element.ref.addEventListener(key, (_: dom.Event) => fun())
  }

  given Fun1Binder[Event <: dom.Event]: AttributeBinder[Event => Unit] = { (element, namespaceURI, key, fun) =>
    element.ref.addEventListener(key, (ev: Event) => fun(ev))
  }

  given FunValueBinder: AttributeBinder[String => Unit] = { (element, namespaceURI, key, fun) =>
    element.ref.addEventListener(
      key,
      (ev: dom.Event) => fun(DomApi.getValue(ev.target.asInstanceOf[dom.Element]).getOrElse("")))
  }

  given FunCheckedBinder: AttributeBinder[Boolean => Unit] = { (element, namespaceURI, key, fun) =>
    element.ref.addEventListener(
      key,
      (ev: dom.Event) => fun(DomApi.getChecked(ev.target.asInstanceOf[dom.Element]).getOrElse(false)))
  }

  given FunFilesBinder: AttributeBinder[List[dom.File] => Unit] = { (element, namespaceURI, key, fun) =>
    element.ref.addEventListener(
      key,
      (ev: dom.Event) =>
        fun {
          DomApi.getFiles(ev.target.asInstanceOf[dom.Element]).getOrElse(Nil)
        })
  }

  given FunTargetBinder[Ref <: dom.EventTarget]: AttributeBinder[Ref => Unit] = { (element, namespaceURI, key, fun) =>
    element.ref.addEventListener(
      key,
      (ev: dom.Event) =>
        fun {
          ev.target.asInstanceOf[Ref]
        })
  }
//
//  given EventListenerBinder[Ev <: dom.Event, Out]: AttributeBinder[EventListener[Ev, Out]] = {
//    (element, namespaceURI, key, listener) =>
//      listener.apply(element)
//  }

  given htmlModBinder[Ref <: Node]: AttributeBinder[Ref] = { (element, namespaceURI, key, mod) =>
    mod.apply(element)
  }
}
