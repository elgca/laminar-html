package scala.xml
package binders



object Attrs {

  def unapply(e: String): Option[String] = {
    allAttrs
      .find(key => key.equalsIgnoreCase(e))
      .orElse(Seq(e).find(x => complexAttrs(x)))
  }

  def isComposite(attrKey: String): Boolean = {
    compositeHtmlAttr.contains(attrKey)
  }

  object StringAttr {

    def unapply(e: String): Boolean = {
      stringAttrs.contains(e) ||
      stringSvgAttrs.contains(e) ||
      complexAttrs(e)
    }
  }

  object BooleanAttr {

    def unapply(e: String): Boolean = {
      boolAsTrueFalseAttrs.contains(e) || boolAsOnOffAttrs.contains(e)
    }
  }

  object IntAttr {

    def unapply(e: String): Boolean = {
      intAttrs.contains(e) || intSvgAttr.contains(e)
    }
  }

  object DoubleAttr {

    def unapply(e: String): Boolean = { doubleSvgAttr.contains(e) }
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

  // svg keys
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

  val allAttrs = {
    stringAttrs ++ intAttrs ++ boolAsOnOffAttrs ++ boolAsTrueFalseAttrs ++ compositeHtmlAttr
      ++ intSvgAttr ++ doubleSvgAttr ++ stringSvgAttrs
  }
}
