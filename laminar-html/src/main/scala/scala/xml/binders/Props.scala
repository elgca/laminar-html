package scala.xml
package binders

object Props {

  def unapply(e: String): Option[String] = {
    allProps.find(key => key.equalsIgnoreCase(e))
  }

  object StringProp {
    def unapply(x: String): Boolean = stringProps.contains(x)
    def apply(x: String): Boolean   = stringProps.contains(x)
  }

  object BoolProp {
    def unapply(x: String)        = boolProps.contains(x)
    def apply(x: String): Boolean = boolProps.contains(x)
  }

  object DoubleProp {
    def unapply(x: String)        = doubleProps.contains(x)
    def apply(x: String): Boolean = doubleProps.contains(x)
  }

  object IntProp {
    def unapply(x: String)        = intProps.contains(x)
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
