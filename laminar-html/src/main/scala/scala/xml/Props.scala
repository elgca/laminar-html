package scala.xml

object Props {

  /** 这些属性 {{{ 不是调用 element.setAttribute(key,value) 应该采用
    * element.<key> = value }}}
    *
    * [[https://stackoverflow.com/a/6004028/2601788]]
    */
  inline def contains(inline key: String): Boolean = props.contains(key)

  val props = Set(
    "indeterminate",
    "checked",
    "selected",
    "value",
    "accept",
    "action",
    "accessKey",
    "alt",
    "autocapitalize",
    "autocomplete",
    "autofocus",
    "cols",
    "colSpan",
    "content",
    "defaultChecked",
    "defaultSelected",
    "defaultValue",
    "dir",
    "disabled",
    "download",
    "draggable",
    "enctype",
    "htmlFor",
    "formEnctype",
    "formMethod",
    "formNoValidate",
    "formTarget",
    "hidden",
    "high",
    "httpEquiv",
    "id",
    "inputMode",
    "label",
    "lang",
    "loading",
    "low",
    "minLength",
    "maxLength",
    "media",
    "method",
    "multiple",
    "name",
    "noValidate",
    "optimum",
    "pattern",
    "placeholder",
    "readOnly",
    "required",
    "rows",
    "rowSpan",
    "scoped",
    "size",
    "slot",
    "spellcheck",
    "tabIndex",
    "target",
    "title",
    "translate",
    "xmlns",
    "crossOrigin",
  )
}
