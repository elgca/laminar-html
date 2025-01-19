package scala.xml
package binders

/** 自定义的属性绑定类, 提供对应的隐式转换实现自定义的类型绑定
  * {{{
  *   given helloAttr:AttributeBinder["hello",String] = ....
  *   <div
  *      hello = "xxx"
  *   />
  * }}}
  */
trait AttributeBinder[T <: String, DomValue] {

  def bindAttr(
    element: ReactiveElementBase,
    namespaceURI: Option[String],
    prefix: Option[String],
    key: String,
    value: DomValue | scala.Null,
  ): Unit
}
