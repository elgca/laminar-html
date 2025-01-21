package scala.xml
package binders

import scala.quoted.*
import scala.util.Try as ScalaTry
import scala.xml.MacorsMessage.????

object Attrs {
  import MacorsMessage.AttrType
  given infoType: AttrType = AttrType("HtmlAttrs")

  def unapply(e: String): Option[String] = {
    allAttrs
      .find(key => key.equalsIgnoreCase(e))
      .orElse(Seq(e).find(x => complexAttrs(x)))
  }

  def isComposite(attrKey: String): Boolean = {
    compositeHtmlAttr.contains(attrKey)
  }

  class AttrMacros[R](
    constFormat: String | scala.Null => String | scala.Null,
    encode: Expr[R => String | scala.Null],
    validType: Type[?],
  )(using
    quotes: Quotes,
    propToExpr: ToExpr[R],
    propType: Type[R],
  )(using AttrType) {
    def this(
      constFormat: String | scala.Null => String | scala.Null,
      encode: Expr[R => String | scala.Null],
    )(using
      quotes: Quotes,
      propToExpr: ToExpr[R],
      propType: Type[R],
    )(using AttrType) = {
      this(constFormat, encode, propType)
    }

    import quotes.*
    import quotes.reflect.*

    def checkType[T: Type]: Boolean = {
      if TypeRepr.of[T] =:= TypeRepr.of[Text] then true
      else if TypeRepr.of[T] <:< TypeRepr.of[R] || TypeRepr.of[T] <:< TypeRepr.of[Option[R]] then true
      else false
    }

    private def block[T: Type](body: => Expr[MetatDataBinder])(using MacrosPosition): Expr[MetatDataBinder] = {
      if !checkType[T] then MacorsMessage.expectationType[T, R | Option[R]]
      MacorsMessage.showSupportedTypes[R | Option[R]]
      body
    }

    def withConst[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      name: String,
      constStr: String | scala.Null,
    )(using MacrosPosition): Expr[MetatDataBinder] = block[T] {
      // 检查常量类型对于 <div cc={"xxx"} /> 需要检查类型, 以确保类型准确
      ScalaTry(constFormat(constStr))
        .map(r => {
          '{ AttrsApi.setHtmlAttributeBinder(${ namespace }, ${ Expr(name) }, ${ Expr(r) }) }
        })
        .getOrElse(MacorsMessage.unsupportConstProp(constStr)(using validType))
    }

    def withExpr[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      name: String,
      expr: Expr[T],
    )(using MacrosPosition): Expr[MetatDataBinder] = block[T] {
      val attrValue: Expr[String | Null] = expr match {
        case v: Expr[R] @unchecked if TypeRepr.of[T] <:< TypeRepr.of[R]                    => {
          '{ ${ encode }.apply(${ v }) }
        }
        case optV: Expr[Option[R]] @unchecked if TypeRepr.of[T] <:< TypeRepr.of[Option[R]] => {
          '{ ${ optV }.map(v => ${ encode }.apply(v)).orNull }
        }

        case _ => MacorsMessage.expectationType[T, R | Option[R]]
      }
      '{ AttrsApi.setHtmlAttributeBinder(${ namespace }, ${ Expr(name) }, ${ attrValue }) }
    }
  }

  object AttrMacros {
    import com.raquo.laminar.codecs.*

    def StringAttr(using Quotes, AttrType) = AttrMacros[String](x => x, '{ StringAsIsCodec.encode })

    def TrueFalseAttr(using Quotes) =
      AttrMacros[Boolean](nilsafe(_.toBoolean.toString), '{ BooleanAsTrueFalseStringCodec.encode })

    def OnOffAttr(using Quotes) = AttrMacros[Boolean](
      nilsafe(x => if Seq("true", "false", "on", "off").contains(x) then x else raiseError),
      '{ BooleanAsOnOffStringCodec.encode },
      Type.of["true" | "false" | "on" | "off"],
    )

    def IntAttr(using Quotes) = AttrMacros[Int](nilsafe(_.toInt.toString), '{ IntAsStringCodec.encode })

    def DoubleAttr(using Quotes) = AttrMacros[Double](nilsafe(_.toDouble.toString), '{ DoubleAsStringCodec.encode })

    def unapply(e: String)(using Quotes): Option[AttrMacros[?]] = {
      Attrs.unapply(e).filterNot(isComposite).map {
        case attrKey if isStringAttr(attrKey)    => { StringAttr } // string必须在最前面, 有些属性在svg和html中重复了, 如果不一致,则优先string
        case attrKey if isTrueFalseAttr(attrKey) => { TrueFalseAttr }
        case attrKey if isOnOffAttr(attrKey)     => { OnOffAttr }
        case attrKey if isDoubleAttr(attrKey)    => { DoubleAttr }
        case attrKey if isIntAttr(attrKey)       => { IntAttr }
      }
    }

    def withKey(e: String)(using Quotes): AttrMacros[?] = {
      unapply(e).getOrElse(????)
    }

    private def raiseError: Nothing = ???

    private def nilsafe(f: String => String | scala.Null): String | scala.Null => String | scala.Null = {
      (s: String | Null) => if s == null then s else f(s)
    }
  }

  class CompositeAttrMacros(val attrKey: String)(using quotes: Quotes) {
    import quotes.*
    import quotes.reflect.*

    import AttrsApi.*

    private def block[T: Type](body: => Expr[MetatDataBinder])(using MacrosPosition): Expr[MetatDataBinder] = {
      if TypeRepr.of[T] =:= TypeRepr.of[Text] then {} else if normalizer[T].isEmpty then
        MacorsMessage.expectationType[T, CompositeNormalize.CompositeValidTypes]
      MacorsMessage.showSupportedTypes[CompositeNormalize.CompositeValidTypes]
      body
    }

    def normalizer[T: Type]: Option[Expr[CompositeNormalize[T]]] = Expr.summon[CompositeNormalize[T]]

    def withConst[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      name: String,
      constStr: Seq[String],
    )(using MacrosPosition): Expr[MetatDataBinder] = block[T] {
      val items = constStr.flatMap(_.split(" ")).filter(_.nonEmpty).toList
      '{ setCompositeAttributeBinder(${ namespace }, ${ Expr(name) }, ${ Expr(items) }, Nil) }
    }

    def withExpr[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      name: String,
      expr: Expr[T],
    )(using MacrosPosition): Expr[MetatDataBinder] = block[T] {
      val itemsToAdd = '{ ${ normalizer[T].get }.apply(${ expr }) }
      '{ AttrsApi.setCompositeAttributeBinder(${ namespace }, ${ Expr(name) }, ${ itemsToAdd }, Nil) }
    }

    def withExprFromSource[V: Type, CC <: Source[V]: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      name: String,
      sourceValue: Expr[CC],
    )(using MacrosPosition): Expr[MetatDataBinder] = block[V] {
      '{
        setCompositeAttributeBinderFromSource(
          ${ namespace },
          ${ Expr(name) },
          ${ sourceValue },
          ${ normalizer[V].get },
        )
      }
    }
  }

  object CompositeAttrMacros {

    def unapply(e: String)(using Quotes): Option[CompositeAttrMacros] = {
      Attrs.unapply(e).filter(isComposite).map { key =>
        new CompositeAttrMacros(key)
      }
    }

    def apply(key: String)(using quotes: Quotes): CompositeAttrMacros = { new CompositeAttrMacros(key) }
  }

  trait AttrProvider[T] {
    def apply(using Quotes): AttrMacros[?]
  }

  def isStringAttr(e: String): Boolean = stringAttrs.contains(e) || stringSvgAttrs.contains(e) || complexAttrs(e)

  def isIntAttr(e: String): Boolean = intAttrs.contains(e) || intSvgAttr.contains(e)

  def isDoubleAttr(e: String): Boolean = doubleSvgAttr.contains(e)

  def isTrueFalseAttr(e: String): Boolean = boolAsTrueFalseAttrs.contains(e)

  def isOnOffAttr(e: String): Boolean = boolAsOnOffAttrs.contains(e)

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
