package scala.xml
package binders

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
}

object Props {

  def unapply(e: String): Option[String] = {
    Some(e)
      .filter(allProps.contains)
  }

  object StringProp {
    def unapply(x: String): Option[String] = Some(x).filter(stringProps.contains)
    def apply(x: String): Boolean          = stringProps.contains(x)
  }

  object BoolProp {
    def unapply(x: String)        = Some(x).filter(boolProps.contains)
    def apply(x: String): Boolean = boolProps.contains(x)
  }

  object DoubleProp {
    def unapply(x: String)        = Some(x).filter(doubleProps.contains)
    def apply(x: String): Boolean = doubleProps.contains(x)
  }

  object IntProp {
    def unapply(x: String)        = Some(x).filter(intProps.contains)
    def apply(x: String): Boolean = intProps.contains(x)
  }

  val stringProps = Set(
    "value",
    "accept",
    "action",
    "accessKey",
    "alt",
    "autocapitalize",
    "autocomplete",
    "content",
    "defaultValue",
    "dir",
    "download",
    "enctype",
    "htmlFor",
    "formEnctype",
    "formMethod",
    "formTarget",
    "httpEquiv",
    "id",
    "inputMode",
    "label",
    "lang",
    "loading",
    "media",
    "method",
    "name",
    "pattern",
    "placeholder",
    "slot",
    "target",
    "title",
    "xmlns",
    "crossOrigin",
  )

  val boolProps = Set(
    "indeterminate",
    "checked",
    "selected",
    "autofocus",
    "defaultChecked",
    "defaultSelected",
    "disabled",
    "draggable",
    "formNoValidate",
    "hidden",
    "multiple",
    "noValidate",
    "readOnly",
    "required",
    "scoped",
    "spellcheck",
    "translate",
  )

  val doubleProps = Set(
    "high",
    "low",
    "optimum",
  )

  val intProps = Set(
    "cols",
    "colSpan",
    "minLength",
    "maxLength",
    "rows",
    "rowSpan",
    "size",
    "tabIndex",
  )

  val allProps = stringProps ++ boolProps ++ intProps ++ doubleProps ++ intProps
}
