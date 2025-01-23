package scala.xml
package binders

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.quoted.*
import scala.util.Try as ScalaTry
import scala.xml.MacorsMessage.????

object Attrs {
  import AttrsApi.*
  import MacorsMessage.AttrType

  def unapply[T](
    tuple: (Option[String], String, Type[T]),
  )(using quotes: Quotes): Option[(String, AttrMacrosDef[?])] = {
    val (prefix: Option[String], attrKey: String, tpe: Type[T]) = tuple
    attributeDefine.value.iterator
      .flatMap { case (pfx, attr, factory) =>
        attr(attrKey)
          .filter(_ => prefix == pfx)
          .map(key => key -> factory(quotes))
      }
      .headOrMatch(_._2.checkType(using tpe))
  }

  def unknownAttribute(
    prefix: Option[String],
    name: String,
  )(using quotes: Quotes): HtmlAttrMacros[?] = {
    import quotes.reflect._
    given attrType: AttrType = AttrType(s"Unknown attribute :<${prefix.map(_ + ":").getOrElse("")}${name}>")
    HtmlAttrMacros.StringAttr
  }

  class HtmlAttrMacros[R](
    constFormat: String => String,
    encode: Expr[R => String],
    validType: Type[?],
  )(using quotes: Quotes, rTpe: Type[R], attrTpe: AttrType)
      extends AttrMacrosDef[R | Option[R]] {

    import quotes.*
    import quotes.reflect.*

    override def withConstImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      constStr: String)(using MacrosPosition): Expr[MetatDataBinder] = {
      val name = prefix.map(_ + ":" + attrKey).getOrElse(attrKey)
      ScalaTry(constFormat(constStr))
        .map(r => {
          '{ AttrsApi.setHtmlAttributeBinder(${ namespace }, ${ Expr(name) }, ${ Expr(r) }) }
        })
        .getOrElse(MacorsMessage.unsupportConstProp(constStr)(using validType))
    }

    override def withExprImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      expr: Expr[T])(using MacrosPosition): Expr[MetatDataBinder] = {
      val name                           = prefix.map(_ + ":" + attrKey).getOrElse(attrKey)
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

  object HtmlAttrMacros {

    def apply[R: Type](
      constFormat: String => String,
      encode: Expr[R => String],
    )(using quotes: Quotes, attrTpe: AttrType) = {
      new HtmlAttrMacros[R](constFormat, encode, Type.of[R])
    }

    def apply[R](
      constFormat: String => String,
      encode: Expr[R => String],
      validType: Type[?],
    )(using
      quotes: Quotes,
      propToExpr: ToExpr[R],
      propType: Type[R],
    )(using AttrType) = {
      new HtmlAttrMacros[R](constFormat, encode, validType)
    }

    import com.raquo.laminar.codecs.*

    def StringAttr(using Quotes, AttrType) = HtmlAttrMacros[String](x => x, '{ StringAsIsCodec.encode })

    def BoolTrueFalseAttr(using Quotes, AttrType) =
      HtmlAttrMacros[Boolean]((_.toBoolean.toString), '{ BooleanAsTrueFalseStringCodec.encode })

    def BoolOnOffAttr(using Quotes, AttrType) = HtmlAttrMacros[Boolean](
      (x => if Seq("true", "false", "on", "off").contains(x) then x else ????),
      '{ BooleanAsOnOffStringCodec.encode },
      Type.of["true" | "false" | "on" | "off"],
    )

    def IntAttr(using Quotes, AttrType) = HtmlAttrMacros[Int]((_.toInt.toString), '{ IntAsStringCodec.encode })

    def DoubleAttr(using Quotes, AttrType) =
      HtmlAttrMacros[Double]((_.toDouble.toString), '{ DoubleAsStringCodec.encode })

  }

  class CompositeAttrMacros(using quotes: Quotes, attrTpe: AttrType)
      extends AttrMacrosDef[CompositeNormalize.CompositeValidTypes] {

    import quotes.*
    import quotes.reflect.*

    override def checkType[T](using tpe: Type[T]): Boolean = {
      if TypeRepr.of[T] =:= TypeRepr.of[Text] then true
      else if normalizer[T](using tpe).nonEmpty then true
      else false
    }

    def normalizer[T](using tpe: Type[T]): Option[Expr[CompositeNormalize[T]]] = {
      import quotes.reflect.*
      Implicits.search(TypeRepr.of[CompositeNormalize[T]]) match {
        case iss: ImplicitSearchSuccess => Some(iss.tree.asExpr.asInstanceOf[Expr[CompositeNormalize[T]]])
        case isf: ImplicitSearchFailure => None
      }
//      Expr.summon[CompositeNormalize[T]]
    }

    override def withConstImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      constStr: String)(using MacrosPosition): Expr[MetatDataBinder] = {
      val name  = prefix.map(_ + ":" + attrKey).getOrElse(attrKey)
      val items = constStr.split(" ").filter(_.nonEmpty).toList
      '{ setCompositeAttributeBinder(${ namespace }, ${ Expr(name) }, ${ Expr(items) }, Nil) }
    }

    override def withExprImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      expr: Expr[T])(using MacrosPosition): Expr[MetatDataBinder] = {
      val name       = prefix.map(_ + ":" + attrKey).getOrElse(attrKey)
      val itemsToAdd = '{ ${ normalizer[T].get }.apply(${ expr }) }
      '{ AttrsApi.setCompositeAttributeBinder(${ namespace }, ${ Expr(name) }, ${ itemsToAdd }, Nil) }
    }

    override def withExprFromSourceImpl[V: Type, CC <: Source[V]: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      sourceValue: Expr[CC])(using MacrosPosition): Expr[MetatDataBinder] = block[V] {
      val name = prefix.map(_ + ":" + attrKey).getOrElse(attrKey)

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

    def apply(using quotes: Quotes, attrTpe: AttrType): CompositeAttrMacros = {
      new CompositeAttrMacros()
    }
  }

  // ------------ Define --------------
  val attributeDefine = new AttributeDefine(ListBuffer.empty)
    with HtmlAttrs
    with AriaAttrs
    with SvgAttrs
    with ComplexHtmlKeys
    with ComplexSvgKeys

  class AttributeDefine(val value: ListBuffer[(Option[String], String => Option[String], AttrMacrosDefFactory)]) {

    def attrType(name: String)(using pos: MacrosPosition) = {
      val posName = pos.name.value.split('.').dropRight(1).lastOption.getOrElse(pos.showNane.value)
      AttrType(s"${posName}:<${name}>")
    }

    def addOne(e: (Option[String], String => Option[String], AttrMacrosDefFactory)): Unit = value.addOne(e)

    private given Conversion[String, String => Option[String]] = (str: String) =>
      (str2: String) => {
        if str.equals(str2) then Some(str)
        else None
      }

    def boolAsOnOffAttr(name: String)(using pos: MacrosPosition) = addOne {
      (None, name, (quotes: Quotes) => HtmlAttrMacros.BoolOnOffAttr(using quotes, attrType(name)))
    }

    def boolAsTrueFalseAttr(name: String)(using pos: MacrosPosition) = addOne {
      (None, name, (quotes: Quotes) => HtmlAttrMacros.BoolTrueFalseAttr(using quotes, attrType(name)))
    }

    def intAttr(name: String)(using pos: MacrosPosition) = addOne {
      (None, name, (quotes: Quotes) => HtmlAttrMacros.IntAttr(using quotes, attrType(name)))
    }

    def doubleAttr(name: String)(using pos: MacrosPosition) = addOne {
      (None, name, (quotes: Quotes) => HtmlAttrMacros.DoubleAttr(using quotes, attrType(name)))
    }

    def stringAttr(name: String)(using pos: MacrosPosition) = addOne {
      (None, name, (quotes: Quotes) => HtmlAttrMacros.StringAttr(using quotes, attrType(name)))
    }

    def stringCompositeAttr(name: String, separator: String)(using pos: MacrosPosition) = addOne {
      (None, name, (quotes: Quotes) => CompositeAttrMacros(using quotes, attrType(name)))
    }

    def stringNsAttr(name: String, namespace: String)(using pos: MacrosPosition) = addOne {
      (
        Option(namespace),
        name,
        (quotes: Quotes) => HtmlAttrMacros.StringAttr(using quotes, attrType(s"[${namespace}:${name}]")))
    }

  }

  // ------------ HtmlAttrs ------------
  /** [[com.raquo.laminar.defs.attrs.HtmlAttrs]] */
  trait HtmlAttrs { self: AttributeDefine =>
    def boolAsOnOffHtmlAttr     = boolAsOnOffAttr
    def boolAsTrueFalseHtmlAttr = boolAsTrueFalseAttr
    def intHtmlAttr             = intAttr
    def stringHtmlAttr          = stringAttr

    stringHtmlAttr("charset")
    boolAsTrueFalseHtmlAttr("contenteditable")
    stringHtmlAttr("contextmenu")
    stringHtmlAttr("dropzone")
    stringHtmlAttr("formaction")
    stringHtmlAttr("form")
    intHtmlAttr("height")
    stringHtmlAttr("href")
    stringHtmlAttr("list")
    stringHtmlAttr("max")
    stringHtmlAttr("min")
    stringHtmlAttr("src")
    stringHtmlAttr("step")
    stringHtmlAttr("type")
    boolAsOnOffHtmlAttr("unselectable")
    intHtmlAttr("width")
  }

  // -------- AriaAttrs --------------
  /** [[com.raquo.laminar.defs.attrs.AriaAttrs]] */
  trait AriaAttrs { self: AttributeDefine =>

    def boolAsTrueFalseAriaAttr(name: String)(using MacrosPosition) = boolAsTrueFalseAttr("aria-" + name)
    def doubleAriaAttr(name: String)(using MacrosPosition)          = doubleAttr("aria-" + name)
    def intAriaAttr(name: String)(using MacrosPosition)             = intAttr("aria-" + name)
    def stringAriaAttr(name: String)(using MacrosPosition)          = stringAttr("aria-" + name)

    stringAriaAttr("activedescendant")
    boolAsTrueFalseAriaAttr("atomic")
    stringAriaAttr("autocomplete")
    boolAsTrueFalseAriaAttr("busy")
    stringAriaAttr("checked")
    stringAriaAttr("controls")
    stringAriaAttr("current")
    stringAriaAttr("describedby")
    boolAsTrueFalseAriaAttr("disabled")
    stringAriaAttr("dropeffect")
    boolAsTrueFalseAriaAttr("expanded")
    stringAriaAttr("flowto")
    boolAsTrueFalseAriaAttr("grabbed")
    boolAsTrueFalseAriaAttr("haspopup")
    boolAsTrueFalseAriaAttr("hidden")
    stringAriaAttr("invalid")
    stringAriaAttr("label")
    stringAriaAttr("labelledby")
    intAriaAttr("level")
    stringAriaAttr("live")
    boolAsTrueFalseAriaAttr("multiline")
    boolAsTrueFalseAriaAttr("multiselectable")
    stringAriaAttr("orientation")
    stringAriaAttr("owns")
    intAriaAttr("posinset")
    stringAriaAttr("pressed")
    boolAsTrueFalseAriaAttr("readonly")
    stringAriaAttr("relevant")
    boolAsTrueFalseAriaAttr("required")
    boolAsTrueFalseAriaAttr("selected")
    intAriaAttr("setsize")
    stringAriaAttr("sort")
    doubleAriaAttr("valuemax")
    doubleAriaAttr("valuemin")
    doubleAriaAttr("valuenow")
    stringAriaAttr("valuetext")
  }

  // -------- SvgAttrs --------------
  /** [[com.raquo.laminar.defs.attrs.SvgAttrs]] */
  trait SvgAttrs { self: AttributeDefine =>
    def doubleSvgAttr = doubleAttr
    def intSvgAttr    = intAttr
    def stringSvgAttr = stringAttr

    doubleSvgAttr("accent-height")
    stringSvgAttr("accumulate")
    stringSvgAttr("additive")
    stringSvgAttr("alignment-baseline")
    doubleSvgAttr("ascent")
    stringSvgAttr("attributeName")
    stringSvgAttr("attributeType")
    doubleSvgAttr("azimuth")
    stringSvgAttr("baseFrequency")
    stringSvgAttr("baseline-shift")
    stringSvgAttr("begin")
    doubleSvgAttr("bias")
    stringSvgAttr("calcMode")
    stringSvgAttr("clip")
    stringSvgAttr("clip-path")
    stringSvgAttr("clipPathUnits")
    stringSvgAttr("clip-rule")
    stringSvgAttr("color")
    stringSvgAttr("color-interpolation")
    stringSvgAttr("color-interpolation-filters")
    stringSvgAttr("color-profile")
    stringSvgAttr("color-rendering")
    stringSvgAttr("contentScriptType")
    stringSvgAttr("contentStyleType")
    stringSvgAttr("cursor")
    stringSvgAttr("cx")
    stringSvgAttr("cy")
    stringSvgAttr("d")
    stringSvgAttr("diffuseConstant")
    stringSvgAttr("direction")
    stringSvgAttr("display")
    stringSvgAttr("divisor")
    stringSvgAttr("dominant-baseline")
    stringSvgAttr("dur")
    stringSvgAttr("dx")
    stringSvgAttr("dy")
    stringSvgAttr("edgeMode")
    doubleSvgAttr("elevation")
    stringSvgAttr("end")
    stringSvgAttr("externalResourcesRequired")
    stringSvgAttr("fill")
    stringSvgAttr("fill-opacity")
    stringSvgAttr("fill-rule")
    stringSvgAttr("filter")
    stringSvgAttr("filterRes")
    stringSvgAttr("filterUnits")
    stringSvgAttr("flood-color")
    stringSvgAttr("flood-opacity")
    stringSvgAttr("font-family")
    stringSvgAttr("font-size")
    stringSvgAttr("font-size-adjust")
    stringSvgAttr("font-stretch")
    stringSvgAttr("font-variant")
    stringSvgAttr("font-weight")
    stringSvgAttr("from")
    stringSvgAttr("gradientTransform")
    stringSvgAttr("gradientUnits")
    stringSvgAttr("height")
    stringSvgAttr("href")
    stringSvgAttr("imageRendering")
    stringSvgAttr("id")
    stringSvgAttr("in")
    stringSvgAttr("in2")
    doubleSvgAttr("k1")
    doubleSvgAttr("k2")
    doubleSvgAttr("k3")
    doubleSvgAttr("k4")
    stringSvgAttr("kernelMatrix")
    stringSvgAttr("kernelUnitLength")
    stringSvgAttr("kerning")
    stringSvgAttr("keySplines")
    stringSvgAttr("keyTimes")
    stringSvgAttr("letter-spacing")
    stringSvgAttr("lighting-color")
    stringSvgAttr("limitingConeAngle")
    stringSvgAttr("local")
    stringSvgAttr("marker-end")
    stringSvgAttr("marker-mid")
    stringSvgAttr("marker-start")
    stringSvgAttr("markerHeight")
    stringSvgAttr("markerUnits")
    stringSvgAttr("markerWidth")
    stringSvgAttr("maskContentUnits")
    stringSvgAttr("maskUnits")
    stringSvgAttr("mask")
    stringSvgAttr("max")
    stringSvgAttr("min")
    stringSvgAttr("mode")
    intSvgAttr("numOctaves")
    stringSvgAttr("offset")
    stringSvgAttr("orient")
    stringSvgAttr("opacity")
    stringSvgAttr("operator")
    stringSvgAttr("order")
    stringSvgAttr("overflow")
    stringSvgAttr("paint-order")
    stringSvgAttr("pathLength")
    stringSvgAttr("patternContentUnits")
    stringSvgAttr("patternTransform")
    stringSvgAttr("patternUnits")
    stringSvgAttr("pointer-events")
    stringSvgAttr("points")
    stringSvgAttr("pointsAtX")
    stringSvgAttr("pointsAtY")
    stringSvgAttr("pointsAtZ")
    stringSvgAttr("preserveAlpha")
    stringSvgAttr("preserveAspectRatio")
    stringSvgAttr("primitiveUnits")
    stringSvgAttr("r")
    stringSvgAttr("radius")
    stringSvgAttr("refX")
    stringSvgAttr("refY")
    stringSvgAttr("repeatCount")
    stringSvgAttr("repeatDur")
    stringSvgAttr("requiredFeatures")
    stringSvgAttr("restart")
    stringSvgAttr("result")
    stringSvgAttr("rx")
    stringSvgAttr("ry")
    stringSvgAttr("scale")
    doubleSvgAttr("seed")
    stringSvgAttr("shape-rendering")
    doubleSvgAttr("specularConstant")
    doubleSvgAttr("specularExponent")
    stringSvgAttr("spreadMethod")
    stringSvgAttr("stdDeviation")
    stringSvgAttr("stitchTiles")
    stringSvgAttr("stop-color")
    stringSvgAttr("stop-opacity")
    stringSvgAttr("stroke")
    stringSvgAttr("stroke-dasharray")
    stringSvgAttr("stroke-dashoffset")
    stringSvgAttr("stroke-linecap")
    stringSvgAttr("stroke-linejoin")
    stringSvgAttr("stroke-miterlimit")
    stringSvgAttr("stroke-opacity")
    stringSvgAttr("stroke-width")
    stringSvgAttr("style")
    stringSvgAttr("surfaceScale")
    stringSvgAttr("tabindex")
    stringSvgAttr("target")
    stringSvgAttr("targetX")
    stringSvgAttr("targetY")
    stringSvgAttr("text-anchor")
    stringSvgAttr("text-decoration")
    stringSvgAttr("text-rendering")
    stringSvgAttr("to")
    stringSvgAttr("transform")
    stringSvgAttr("type")
    stringSvgAttr("values")
    stringSvgAttr("viewBox")
    stringSvgAttr("visibility")
    stringSvgAttr("width")
    stringSvgAttr("word-spacing")
    stringSvgAttr("writing-mode")
    stringSvgAttr("x")
    stringSvgAttr("x1")
    stringSvgAttr("x2")
    stringSvgAttr("xChannelSelector")
    stringNsAttr("href", namespace = "xlink")
    stringNsAttr("role", namespace = "xlink")
    stringNsAttr("title", namespace = "xlink")
    stringNsAttr("space", namespace = "xml")
    stringSvgAttr("xmlns")
    stringNsAttr("xlink", namespace = "xmlns")
    stringSvgAttr("y")
    stringSvgAttr("y1")
    stringSvgAttr("y2")
    stringSvgAttr("yChannelSelector")
    stringSvgAttr("z")
  }

  // -------- ComplexHtmlKeys --------------
  /** [[com.raquo.laminar.defs.complex.ComplexHtmlKeys]] */
  trait ComplexHtmlKeys { self: AttributeDefine =>

    def dataAttr(attrPrefix: String)(using pos: MacrosPosition) = addOne {
      (
        None,
        x => if x.startsWith(attrPrefix) then Some(x) else None,
        (quotes: Quotes) => HtmlAttrMacros.StringAttr(using quotes, attrType(s"${attrPrefix}-[]")))
    }

    stringAttr("style")
    stringCompositeAttr("class", separator = " ")
    stringCompositeAttr("rel", separator = " ")
    stringCompositeAttr("role", separator = " ")
    dataAttr("data-")
  }

  /** [[com.raquo.laminar.defs.complex.ComplexSvgKeys]] */
  trait ComplexSvgKeys { self: AttributeDefine =>

    def stringCompositeSvgAttr(name: String, separator: String)(using pos: MacrosPosition) =
      stringCompositeAttr(name, separator)

    stringCompositeSvgAttr("class", separator = " ")
    stringCompositeSvgAttr("role", separator = " ")
  }
}
