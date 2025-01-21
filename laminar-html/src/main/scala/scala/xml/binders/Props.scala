package scala.xml
package binders

import scala.quoted.*
import scala.util.Try as ScalaTry
import scala.xml.MacorsMessage.????

object Props {
  given infoType: MacorsMessage.AttrType = MacorsMessage.AttrType("HtmlProps")

  def unapply(e: String): Option[String] = {
    allProps.find(key => key.equalsIgnoreCase(e))
  }

  class PropMacros[R](
    val propKey: String,
    constValueConversion: String => R,
  )(using
    quotes: Quotes,
    propToExpr: ToExpr[R],
    propType: Type[R],
  ) {
    import quotes.reflect.*
    import quotes.*

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
      constStr: String,
    )(using MacrosPosition): Expr[MetatDataBinder] = block[T] {
      ScalaTry(constValueConversion(constStr))
        .map(r => {
          if !checkType[T] then MacorsMessage.expectationType[T, R | Option[R]]
          '{ PropsApi.setHtmlPropertyBinder(${ Expr(propKey) }, ${ Expr(r) }) }
        })
        .getOrElse(MacorsMessage.unsupportConstProp(constStr)(using propType))
    }

    def withExpr[T: Type](
      expr: Expr[T],
    )(using MacrosPosition): Expr[MetatDataBinder] = block[T] {
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
  }

  object PropMacros {

    def unapply(e: String)(using quotes: Quotes): Option[PropMacros[?]] = {
      Props
        .unapply(e)
        .map {
          case propKey @ StringProp() => PropMacros[String](propKey, x => x)
          case propKey @ BoolProp()   => PropMacros[Boolean](propKey, x => x.toBoolean)
          case propKey @ DoubleProp() => PropMacros[Double](propKey, x => x.toDouble)
          case propKey @ IntProp()    => PropMacros[Double](propKey, x => x.toInt)
        }
    }

    def withKey(e: String)(using quotes: Quotes): PropMacros[?] = {
      unapply(e).getOrElse(????)
    }

  }

  def fromSource[T: Type, CC <: Source[T]: Type](
    name: "value",
    sourceValue: Expr[CC],
  )(using quotes: Quotes): Expr[MetatDataBinder] = {
    import quotes.reflect.*
    import quotes.*
    sourceValue match {
      case sourceStr: Expr[Source[String]] @unchecked if TypeRepr.of[T] <:< TypeRepr.of[String] =>
        MacorsMessage.showSupportedTypes[String | Option[String] | Source[String]]
        '{ PropsApi.valuePropUpdater(${ sourceStr }) }

      case _ => MacorsMessage.expectationType[CC, String | Option[String] | Source[String]]
    }
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
