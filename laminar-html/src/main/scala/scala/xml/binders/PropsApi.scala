package scala.xml
package binders

import org.scalajs.dom

import scala.scalajs.js

object PropsApi {

  inline def setHtmlPropertyBinder[DomV](
    inline propKey: String,
    inline value: DomV,
  ): (NamespaceBinding, ReactiveElementBase) => Unit = (*, element) => {
    element.ref.asInstanceOf[js.Dynamic].updateDynamic(propKey)(value.asInstanceOf[js.Any])
  }

  inline def removeHtmlPropertyBinder(
    inline propKey: String,
  ): (NamespaceBinding, ReactiveElementBase) => Unit = (*, element) => {
    element.ref.asInstanceOf[js.Dynamic].updateDynamic(propKey)(js.undefined)
  }

  def getHtmlPropertyRaw[DomV](
    element: dom.Element,
    propKey: String,
  ): js.UndefOr[DomV] = {
    element.asInstanceOf[js.Dynamic].selectDynamic(propKey).asInstanceOf[js.UndefOr[DomV]]
  }

  def setHtmlPropertyRaw[DomV](
    element: dom.Element,
    propKey: String,
    value: DomV,
  ): Unit = {
    element.asInstanceOf[js.Dynamic].updateDynamic(propKey)(value.asInstanceOf[js.Any])
  }

  // value更新的特殊处理函数
  def updateWhenKeyIsValue(
    element: ReactiveElementBase,
    nextValue: String,
  ): Unit = {
    if !getHtmlPropertyRaw[String](element.ref, "value").contains(nextValue) then {
      setHtmlPropertyRaw(element.ref, "value", nextValue)
    }
  }
}
