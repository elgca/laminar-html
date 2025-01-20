package scala.xml
package binders

import org.scalajs.dom

import scala.scalajs.js

object PropsApi {

  inline def setHtmlPropertyBinder[DomV](
    inline propKey: String,
    inline value: DomV,
  ): MetatDataBinder = (*, element) => {
    element.ref.asInstanceOf[js.Dynamic].updateDynamic(propKey)(value.asInstanceOf[js.Any])
  }

  inline def removeHtmlPropertyBinder(
    inline propKey: String,
  ): MetatDataBinder = (*, element) => {
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
  def valuePropUpdater(source: Source[String]): MetatDataBinder = (*, element) => {
    ReactiveElement.bindFn(element, source.toObservable) { nextValue =>
      if !getHtmlPropertyRaw[String](element.ref, "value").contains(nextValue) then {
        setHtmlPropertyRaw(element.ref, "value", nextValue)
      }
    }
  }
}
