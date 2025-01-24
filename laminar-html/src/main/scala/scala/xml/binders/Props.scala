package scala.xml
package binders

import scala.collection.mutable.ListBuffer
import scala.quoted.*
import scala.util.Try as ScalaTry
import scala.xml.MacorsMessage.{????, AttrType}

object Props {
  given infoType: MacorsMessage.AttrType = MacorsMessage.AttrType("HtmlProps")

  def unapply[T](
    tuple: (Option[String], String, Type[T]),
  )(using quotes: Quotes): Option[(String, AttrMacrosDef[?], Seq[(String, String)])] = {
    val (prefix: Option[String], attrKey: String, tpe: Type[T]) = tuple

    val matchedProps = propsDefine.value.iterator.flatMap { case (propName, attr, factory) =>
      attr(attrKey)
        .filter(_ => prefix.isEmpty)
        .map(* => propName -> factory(quotes))
    }.toSeq

    val typeHints = matchedProps.map(_._2.supportedTypesMessage)
    matchedProps
      .headOrMatch(_._2.checkType(using tpe))
      .map(x => (x._1, x._2, typeHints))
  }

  class PropMacros[R](
    val propKey: String,
    constValueConversion: String => R,
  )(using AttrType)(using
    quotes: Quotes,
    propToExpr: ToExpr[R],
    propType: Type[R],
  ) extends AttrMacrosDef[R | Option[R]] {
    import quotes.reflect.*
    import quotes.*

    override protected def withConstImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      constStr: String)(using MacrosPosition): Expr[MetatDataBinder] = {
      ScalaTry(constValueConversion(constStr))
        .map(r => {
          '{ PropsApi.setHtmlPropertyBinder(${ Expr(propKey) }, ${ Expr(r) }) }
        })
        .getOrElse(MacorsMessage.unsupportConstProp[R](constStr))
    }

    override protected def withExprImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      expr: Expr[T])(using MacrosPosition): Expr[MetatDataBinder] = {
      expr match {
        case v: Expr[R] @unchecked if TypeRepr.of[T] <:< TypeRepr.of[R]                    => {
          '{ PropsApi.setHtmlPropertyBinder(${ Expr(propKey) }, ${ v }) }
        }
        case optV: Expr[Option[R]] @unchecked if TypeRepr.of[T] <:< TypeRepr.of[Option[R]] => {
          '{
            ${ optV }.fold(MetaData.EmptyBinder)(str =>
              PropsApi.setHtmlPropertyBinder(
                ${ Expr(propKey) }, {
                  str
                }))
          }
        }

        case _ => MacorsMessage.expectationType[T, R | Option[R]]
      }
    }

    override def withExprFromSourceImpl[V: Type, CC <: Source[V]: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      sourceValue: Expr[CC])(using MacrosPosition): Expr[MetatDataBinder] = {
      if propKey == "value" then {
        import quotes.reflect.*
        import quotes.*
        sourceValue match {
          case sourceStr: Expr[Source[String]] @unchecked if TypeRepr.of[R] <:< TypeRepr.of[String] =>
            '{ PropsApi.valuePropUpdater("value", ${ sourceStr }) }

          case _ => MacorsMessage.expectationType[CC, String | Option[String] | Source[String]]
        }
      } else {
        super.withExprFromSourceImpl(namespace, prefix, attrKey, sourceValue)
      }
    }
  }

  // ------------ Define --------------
  val propsDefine = new PropsDefine() with HtmlProps

  class PropsDefine(
    val value: ListBuffer[(String, String => Option[String], AttrMacrosDefFactory)] = ListBuffer.empty,
  ) {

    def attrType(name: String)(using pos: MacrosPosition) = {
      val posName =
        pos.showNane.value // pos.name.value.split('.').dropRight(1).lastOption.getOrElse(pos.showNane.value)
      AttrType(s"${posName}:<${name}>")
    }

    def propName(key: String) = { (inputKey: String) =>
      if key.equalsIgnoreCase(inputKey) then {
        Some(key)
      } else {
        None
      }
    }

    def boolProp(name: String)(using MacrosPosition): Unit = value.addOne {
      (
        name,
        propName(name),
        (q: Quotes) => { given quotes: Quotes = q; PropMacros[Boolean](name, x => x.toBoolean)(using attrType(name)) },
      )
    }

    def doubleProp(name: String)(using MacrosPosition): Unit = value.addOne {
      (
        name,
        propName(name),
        (q: Quotes) => { given quotes: Quotes = q; PropMacros[Double](name, x => x.toDouble)(using attrType(name)) },
      )
    }

    def intProp(name: String)(using MacrosPosition): Unit = value.addOne {
      (
        name,
        propName(name),
        (q: Quotes) => { given quotes: Quotes = q; PropMacros[Int](name, x => x.toInt)(using attrType(name)) },
      )
    }

    def stringProp(name: String)(using MacrosPosition): Unit = value.addOne {
      (
        name,
        propName(name),
        (q: Quotes) => { given quotes: Quotes = q; PropMacros[String](name, x => x)(using attrType(name)) },
      )
    }
  }

  /** [[com.raquo.laminar.defs.props.HtmlProps]] */
  trait HtmlProps { self: PropsDefine =>
    boolProp("indeterminate")
    boolProp("checked")
    boolProp("selected")
    stringProp("value")
    stringProp("accept")
    stringProp("action")
    stringProp("accessKey")
    stringProp("alt")
    stringProp("autocapitalize")
    stringProp("autocomplete")
    boolProp("autofocus")
    intProp("cols")
    intProp("colSpan")
    stringProp("content")
    boolProp("defaultChecked")
    boolProp("defaultSelected")
    stringProp("defaultValue")
    stringProp("dir")
    boolProp("disabled")
    stringProp("download")
    boolProp("draggable")
    stringProp("enctype")
    stringProp("htmlFor")
    stringProp("formEnctype")
    stringProp("formMethod")
    boolProp("formNoValidate")
    stringProp("formTarget")
    boolProp("hidden")
    doubleProp("high")
    stringProp("httpEquiv")
    stringProp("id")
    stringProp("inputMode")
    stringProp("label")
    stringProp("lang")
    stringProp("loading")
    doubleProp("low")
    intProp("minLength")
    intProp("maxLength")
    stringProp("media")
    stringProp("method")
    boolProp("multiple")
    stringProp("name")
    boolProp("noValidate")
    doubleProp("optimum")
    stringProp("pattern")
    stringProp("placeholder")
    boolProp("readOnly")
    boolProp("required")
    intProp("rows")
    intProp("rowSpan")
    boolProp("scoped")
    intProp("size")
    stringProp("slot")
    boolProp("spellcheck")
    intProp("tabIndex")
    stringProp("target")
    stringProp("title")
    boolProp("translate")
    stringProp("xmlns")
    stringProp("crossOrigin")
  }
}
