//package scala.xml
//
//import com.raquo.airstream.core.{Observable, Source}
//import org.scalajs.dom
//
//import scala.annotation.implicitNotFound
//import scala.collection.immutable.{List, Nil}
//import scala.scalajs.js
//
////trait MetaData {
////
////  def foreach(f: MetaData => Unit): Unit
////
////  def next: MetaData
////
////  def apply(namespaceBinding: NamespaceBinding, element: ReactiveElementBase): Unit = {}
////
////}
//
//case object Null extends MetaData {
//  override def next: MetaData = this
//
//  override def foreach(f: MetaData => Unit): Unit = {}
//}
//
//class UnprefixedAttributeBase[V: AttributeBinder](
//  val key: String,
//  val value: V,
//  val next: MetaData,
//) extends MetaData {
//
//  override def apply(namespaceBinding: NamespaceBinding, element: ReactiveElementBase): Unit = {
//    summon[AttributeBinder[V]].bindAttr(element, None, key, value)
//  }
//
//  override def foreach(f: MetaData => Unit): Unit = {
//    f(this)
//    next.foreach(f)
//  }
//}
//
//class UnprefixedAttribute[V](
//  val key: String,
//  val value: V,
//  val next: MetaData,
//)
//
//object UnprefixedAttribute {
//
//  implicit inline def toMetaData[V: AttributeBinder](inline data: UnprefixedAttribute[V]): MetaData = {
//    val x = MacrosTool.attribute(data)
//    Null
////    new UnprefixedAttributeBase(x.key, x.value, x.next)
//  }
//}
//
//class PrefixedAttribute[V: AttributeBinder](
//  val prefix: String,
//  val key: String,
//  val value: V,
//  val next: MetaData,
//) extends MetaData {
//
//  override def apply(namespaceBinding: NamespaceBinding, element: ReactiveElementBase): Unit = {
//    summon[AttributeBinder[V]].bindAttr(
//      element,
//      namespaceBinding.namespaceURI(prefix),
//      s"$prefix:$key",
//      value,
//    )
//  }
//
//  override def foreach(f: MetaData => Unit): Unit = {
//    f(this)
//    next.foreach(f)
//  }
//}
//
//@implicitNotFound(msg = """无法在 HTML 属性中嵌入类型为 ${T} 的值，未找到隐式的 AttributeBinder[${T}]。
//  以下类型是支持的：
//  - 属性类型:
//    - String
//    - Boolean: false → 移除属性; true → 空属性
//    - Int/Double: _.toString
//    - List[String]: _.mkString(" ")
//    - Option[T]
//  - 反应式变量:
//    - Source[T]: Var[T],Signal[T]等, T为属性类型
//  - 事件函数:
//    - `() => Unit`
//    - `(e: Ev <: dom.Event) => Unit`
//    - `(value:String) => Unit`
//      - 等效于 `(e: dom.Event) => f(e.target.value.getOrElse(""))`
//    - `(checked:Boolean) => Unit`
//      - 等效于 `(e: dom.Event) => f(e.target.checked.getOrElse(false))`
//    - `(files:List[dom.File]) => Unit`
//      - 等效于 `(e: dom.Event) => f(e.target.files.getOrElse(List.empty))`
//    - `Source[ListenerFunction]`
//      - 当监听函数变化,会移除旧监听器
//  """)
//trait AttributeBinder[T] {
//  def bindAttr(element: ReactiveElementBase, namespaceURI: Option[String], key: String, value: T): Unit
//}
//
//object AttributeBinder {
//
//  // 基础数据类型
//  @implicitNotFound(msg = """无法在 XML 属性中嵌入基础类型 ${T}，未找到隐式的 AttrValue[${T}]。
//  以下类型是支持的：
//  - String
//  - Boolean: false → 移除属性; true → 空属性
//  - Int/Double: _.toString
//  - List[String]: _.mkString(" ")
//  - Option[T]
//  """)
//  trait AttrValue[T] {
//    def apply(node: dom.Element, ns: Option[String], key: String, value: T): Unit
//  }
//
//  object AttrValue {
//
//    def removeAttr(node: dom.Element, ns: Option[String], key: String): Unit = {
//      ns.fold(node.removeAttribute(key))(ns => node.removeAttributeNS(ns, key))
//    }
//
//    def setAttribute(node: dom.Element, ns: Option[String], key: String, value: String): Unit = {
//      ns.fold {
//        if Props.contains(key) then setProperty(node, ns, key, value)
//        else node.setAttribute(key, value)
//      } { ns =>
//        node.setAttributeNS(ns, key, value)
//      }
//    }
//
//    def setProperty[DomV](node: dom.Element, ns: Option[String], key: String, value: DomV): Unit = {
//      node.asInstanceOf[js.Dynamic].updateDynamic(key)(value.asInstanceOf[js.Any])
//    }
//
//    def getProperty[DomV](node: dom.Element, key: String): js.UndefOr[DomV] = {
//      node.asInstanceOf[js.Dynamic].selectDynamic(key).asInstanceOf[js.UndefOr[DomV]]
//    }
//
//    given SetAttr: AttrValue[String] = (node, ns, key, value) => setAttribute(node, ns, key, value)
//
//    given BoolSetter: AttrValue[Boolean] = (node, ns, key, value) =>
//      if value then SetAttr(node, ns, key, "") else removeAttr(node, ns, key)
//
//    given DoubleSetter: AttrValue[Double] = (node, ns, key, value) => SetAttr(node, ns, key, value.toString)
//
//    given IntSetter: AttrValue[Int] = (node, ns, key, value) => SetAttr(node, ns, key, value.toString)
//
//    given TextSetter: AttrValue[Text] = (node, ns, key, value) => SetAttr(node, ns, key, value.data)
//
//    given OptionSetter[T, OPT[x] <: Option[x]](using setter: AttrValue[T]): AttrValue[OPT[T]] =
//      (node, ns, key, value) =>
//        value match {
//          case None        => removeAttr(node, ns, key)
//          case Some(value) => setter(node, ns, key, value)
//        }
//
//    given NilSetter: AttrValue[Nil.type] = (node, ns, key, value) => SetAttr(node, ns, key, "")
//
//    given ListSetter[SEQ[x] <: Seq[x]]: AttrValue[SEQ[String]] = (node, ns, key, value) =>
//      SetAttr(node, ns, key, value.map(_.trim).filter(_.isEmpty).mkString(" "))
//  }
//
//  given PrimaryBinder[T](using setter: AttrValue[T]): AttributeBinder[T] = { (element, namespaceURI, key, value) =>
//    setter(element.ref, namespaceURI, key, value)
//  }
//
//  given SourceBinder[T, S[x] <: Source[x]](using setter: AttrValue[T]): AttributeBinder[S[T]] = {
//    (element, namespaceURI, key, value) =>
//      val updater: (dom.Element, Option[String], String, T) => Unit =
//        if key == "value" && namespaceURI.isEmpty then
//          (e: dom.Element, ns: Option[String], key: String, nextValue: T) =>
//            if !AttrValue.getProperty(e, key).contains(nextValue) then AttrValue.setProperty(e, ns, key, nextValue)
//        else if namespaceURI.isEmpty && Props.contains(key) then AttrValue.setProperty
//        else setter.apply
//      ReactiveElement.bindFn(element, value.toObservable) { nextValue =>
//        updater(element.ref, namespaceURI, key, nextValue)
//      }
//  }
//
//  // 事件注册
//  type JsListener = js.Function1[? <: dom.Event, Unit]
//
//  private inline def addEventListener[Event <: dom.Event](
//    element: dom.Element,
//    key: String,
//    fun: JsListener,
//  ): Unit = {
//    // 对于onclick这样的映射到click事件上
//    element.addEventListener(Events.get(key), fun)
//  }
//
//  private inline def removeEventListener[Event <: dom.Event](
//    element: dom.Element,
//    key: String,
//    key: String,
//    fun: JsListener,
//  ): Unit = {
//    // 对于onclick这样的映射到click事件上
//    element.removeEventListener(Events.get(key), fun)
//  }
//
//  @implicitNotFound(msg = """不支持的事件函数,未找到隐式的 ToJsListener[${T}]。
//  - 事件函数:
//    - `() => Unit`
//    - `(e: Ev <: dom.Event) => Unit`
//    - `(value:String) => Unit`
//      - 等效于 `(e: dom.Event) => f(e.target.value.getOrElse(""))`
//    - `(checked:Boolean) => Unit`
//      - 等效于 `(e: dom.Event) => f(e.target.checked.getOrElse(false))`
//    - `(files:List[dom.File]) => Unit`
//      - 等效于 `(e: dom.Event) => f(e.target.files.getOrElse(List.empty))`
//  """)
//  trait ToJsListener[T] {
//    def apply(fun: T): JsListener
//  }
//
//  object ToJsListener {
//    given unit: ToJsListener[() => Unit] = fun => (e: dom.Event) => fun()
//
//    given event[Event <: dom.Event]: ToJsListener[Event => Unit] = fun => fun
//
//    given str: ToJsListener[String => Unit] = fun => { (ev: dom.Event) =>
//      fun(DomApi.getValue(ev.target.asInstanceOf[dom.Element]).getOrElse(""))
//    }
//
//    given checked: ToJsListener[Boolean => Unit] = fun => { (ev: dom.Event) =>
//      fun(DomApi.getChecked(ev.target.asInstanceOf[dom.Element]).getOrElse(false))
//    }
//
//    given file: ToJsListener[List[dom.File] => Unit] = fun => { (ev: dom.Event) =>
//      fun(DomApi.getFiles(ev.target.asInstanceOf[dom.Element]).getOrElse(List.empty))
//    }
//
//  }
//
//  given AddEventListenerBinder[T: ToJsListener]: AttributeBinder[T] = (element, namespaceURI, key, value) =>
//    addEventListener(element.ref, key, summon[ToJsListener[T]](value))
//
//  def listenerObservableBinder(
//    element: ReactiveElementBase,
//    key: String,
//    observable: Observable[JsListener],
//  ) = {
//    var before: Option[JsListener] = None
//    ReactiveElement.bindFn(element, observable) { jsFunc =>
//      before match {
//        case Some(listener) =>
//          removeEventListener(element.ref, key, listener)
//          addEventListener(element.ref, key, jsFunc)
//          before = Some(jsFunc)
//        case None           =>
//          addEventListener(element.ref, key, jsFunc)
//          before = Some(jsFunc)
//      }
//    }
//  }
//
//  given RxEventListenerBinder[S[x] <: Source[x], T: ToJsListener]: AttributeBinder[S[T]] =
//    (element, namespaceURI, key, value) => {
//      listenerObservableBinder(element, key, value.toObservable.map(summon[ToJsListener[T]].apply))
//    }
//
//  // Laminar Mod
//
//  given htmlModBinder[Ref <: Node]: AttributeBinder[Ref] = { (element, namespaceURI, key, mod) =>
//    mod.apply(element)
//  }
//}
