package scala.xml
package binders

import com.raquo.ew.ewArray
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.JSStringOps.*

object AttrsApi {

  inline def setHtmlAttributeBinder(
    inline name: String,
    inline value: String | Null,
  ): (NamespaceBinding, ReactiveElementBase) => Unit = (binding: NamespaceBinding, element: ReactiveElementBase) => {
    if value == null then element.ref.removeAttribute(name)
    else element.ref.setAttribute(name, value.asInstanceOf[String])
  }

  inline def removeHtmlAttributeBinder(
    inline name: String,
  ): (NamespaceBinding, ReactiveElementBase) => Unit = (binding: NamespaceBinding, element: ReactiveElementBase) => {
    element.ref.removeAttribute(name)
  }

  def getHtmlAttributeRaw(
    namespaceURI: String | Null,
    element: dom.Element,
    attrName: String,
  ): js.UndefOr[String] = {
    val domValue: String | Null = element.getAttributeNS(
      namespaceURI = namespaceURI.asInstanceOf[String],
      localName = attrName,
    )
    if domValue != null then {
      domValue
    } else {
      js.undefined
    }
  }

  def setHtmlAttributeRaw(
    element: dom.Element,
//    namespaceURI: String | Null,
    name: String,
    value: String | Null,
  ): js.UndefOr[String] = {
    if value == null then element.removeAttribute(name)
    else element.setAttribute(name, value.asInstanceOf[String])
  }

  def setCompositeAttributeBinder(
    name: String,
    itemsToAdd: List[String],
    itemsToRemove: List[String],
  ): (NamespaceBinding, ReactiveElementBase) => Unit = (binding: NamespaceBinding, element: ReactiveElementBase) => {
    val separator = " "
    val domValue  = getHtmlAttributeRaw(null, element.ref, name).map(normalize(_, separator)).getOrElse(Nil)
    val newItems  = (domValue.filterNot(t => itemsToRemove.contains(t)) ++ itemsToAdd).distinct

    val nextDomValue = newItems.mkString(separator)
    setHtmlAttributeRaw(element.ref, name, nextDomValue)
  }

  /** @param items
    *   non-normalized string with one or more items
    *   separated by `separator`
    * @return
    *   individual values. Note that normalization does NOT
    *   ensure that the items are unique.
    */
  def normalize(items: String, separator: String): List[String] = {
    if items.isEmpty then {
      Nil
    } else {
      items.jsSplit(separator).ew.filter(_.nonEmpty).asScalaJs.toList
    }
  }
}

object Attrs {

  def unapply(e: String): Option[String] = {
    Some(e).filter(x => allAttrs.contains(x) || complexAttrs(x))
  }

  def isComposite(attrKey: String): Boolean = {
    compositeHtmlAttr.contains(attrKey)
  }

  val stringAttrs = Set(
    "charset",
    "contextmenu",
    "dropzone",
    "formaction",
    "form",
    "href",
    "list",
    "max",
    "min",
    "src",
    "step",
    "type",
    "style", // ComplexHtmlKeys
  )

  val intAttrs = Set(
    "height",
    "width",
  )

  val boolAsOnOffAttrs = Set(
    "unselectable",
  )

  val boolAsTrueFalseAttrs = Set(
    "contenteditable",
  )

  // ComplexHtmlKeys
  val compositeHtmlAttr = Set(
    "class",
    "rel",
    "role",
  )

  def complexAttrs(key: String): Boolean = {
    if key.startsWith("data-") then true
    else false
  }

  val intSvgAttr = Set(
    "numOctaves",
  )

  val doubleSvgAttr = Set(
    "accent-height",
    "ascent",
    "azimuth",
    "bias",
    "elevation",
    "k1",
    "k2",
    "k3",
    "k4",
    "seed",
    "specularConstant",
    "specularExponent",
  )

  val stringSvgAttrs = Set(
    "accumulate",
    "additive",
    "alignment-baseline",
    "attributeName",
    "attributeType",
    "baseFrequency",
    "baseline-shift",
    "begin",
    "calcMode",
    "clip",
    "clip-path",
    "clipPathUnits",
    "clip-rule",
    "color",
    "color-interpolation",
    "color-interpolation-filters",
    "color-profile",
    "color-rendering",
    "contentScriptType",
    "contentStyleType",
    "cursor",
    "cx",
    "cy",
    "d",
    "diffuseConstant",
    "direction",
    "display",
    "divisor",
    "dominant-baseline",
    "dur",
    "dx",
    "dy",
    "edgeMode",
    "end",
    "externalResourcesRequired",
    "fill",
    "fill-opacity",
    "fill-rule",
    "filter",
    "filterRes",
    "filterUnits",
    "flood-color",
    "flood-opacity",
    "font-family",
    "font-size",
    "font-size-adjust",
    "font-stretch",
    "font-variant",
    "font-weight",
    "from",
    "gradientTransform",
    "gradientUnits",
    "height",
    "href",
    "imageRendering",
    "id",
    "in",
    "in2",
    "kernelMatrix",
    "kernelUnitLength",
    "kerning",
    "keySplines",
    "keyTimes",
    "letter-spacing",
    "lighting-color",
    "limitingConeAngle",
    "local",
    "marker-end",
    "marker-mid",
    "marker-start",
    "markerHeight",
    "markerUnits",
    "markerWidth",
    "maskContentUnits",
    "maskUnits",
    "mask",
    "max",
    "min",
    "mode",
    "offset",
    "orient",
    "opacity",
    "operator",
    "order",
    "overflow",
    "paint-order",
    "pathLength",
    "patternContentUnits",
    "patternTransform",
    "patternUnits",
    "pointer-events",
    "points",
    "pointsAtX",
    "pointsAtY",
    "pointsAtZ",
    "preserveAlpha",
    "preserveAspectRatio",
    "primitiveUnits",
    "r",
    "radius",
    "refX",
    "refY",
    "repeatCount",
    "repeatDur",
    "requiredFeatures",
    "restart",
    "result",
    "rx",
    "ry",
    "scale",
    "shape-rendering",
    "spreadMethod",
    "stdDeviation",
    "stitchTiles",
    "stop-color",
    "stop-opacity",
    "stroke",
    "stroke-dasharray",
    "stroke-dashoffset",
    "stroke-linecap",
    "stroke-linejoin",
    "stroke-miterlimit",
    "stroke-opacity",
    "stroke-width",
    "style",
    "surfaceScale",
    "tabindex",
    "target",
    "targetX",
    "targetY",
    "text-anchor",
    "text-decoration",
    "text-rendering",
    "to",
    "transform",
    "type",
    "values",
    "viewBox",
    "visibility",
    "width",
    "word-spacing",
    "writing-mode",
    "x",
    "x1",
    "x2",
    "xChannelSelector",
    "xmlns",
    "y",
    "y1",
    "y2",
    "yChannelSelector",
    "z",
  )

  val stringSvgNsAttr = Map(
    "href"  -> "xlink",
    "role"  -> "xlink",
    "title" -> "xlink",
    "space" -> "xml",
    "xlink" -> "xmlns",
  )

  val allAttrs =
    stringAttrs ++ intAttrs ++ boolAsOnOffAttrs ++ boolAsTrueFalseAttrs ++ compositeHtmlAttr ++ intSvgAttr ++ doubleSvgAttr ++ stringSvgAttrs

}
