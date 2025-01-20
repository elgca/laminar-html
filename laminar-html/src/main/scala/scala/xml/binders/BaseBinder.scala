package scala.xml
package binders

/** 自定义的属性绑定类, 提供对应的隐式转换实现自定义的类型绑定
  * {{{
  *   given helloAttr:BaseBinder["hello",String] = ....
  *   <div
  *      hello = "xxx"
  *   />
  * }}}
  */
trait BaseBinder[T <: String, DomValue] {

  def bindAttr(
    element: ReactiveElementBase,
    namespaceURI: Option[String],
    prefix: Option[String],
    key: T,
    value: DomValue | scala.Null,
  ): Unit
}

object BaseBinder {

  // Laminar Mod
  given htmlModBinder[Ref <: AnyNode]: BaseBinder[String, Ref] = { (element, namespaceURI, prefix, key, mod) =>
    mod match
      case null  =>
      case other => other.asInstanceOf[Node].apply(element)
  }
}
