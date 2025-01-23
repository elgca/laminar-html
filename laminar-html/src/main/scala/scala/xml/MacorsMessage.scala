package scala.xml

import java.util.Locale
import scala.annotation.tailrec
import scala.quoted.{Expr, Quotes, Type}

object MacorsMessage {

  val isChinese =
    Seq(Locale.CHINESE, Locale.CHINA, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE).contains(Locale.getDefault)

  val ShowTypeHints = System.getProperty("show_type_hints") != "false"

  def ????(using
    quotes: Quotes,
    position: MacrosPosition,
    attrType: AttrType,
  ): Nothing = raiseError {
    if isChinese then s"还未实现功能/非法分支"
    else s"an implementation is missing"
  }

  def expectationType[T <: AnyKind, Except <: AnyKind](using tpe: Type[T], exceptTpe: Type[Except])(using
    quotes: Quotes,
    position: MacrosPosition,
    attrType: AttrType,
  ): Nothing = raiseError {
    if isChinese then {
      s"""不支持的数据类型: `${formatType[T]}`, 期望以下数据类型:
         |${typeExplain[Except]}
         |""".stripMargin
    } else {
      s"""Unsupported data types: `${formatType[T]}`, The following data types are expected:
         |${typeExplain[Except]}
         |""".stripMargin
    }
  }

  def showSupportedTypes[Except <: AnyKind](using
    exceptTpe: Type[Except],
  )(using
    quotes: Quotes,
    position: MacrosPosition,
    attrType: AttrType,
  ): Unit = logInfo {
    if isChinese then {
      s"""[${attrType}]支持的数据类型:
         |${typeExplain[Except]}
         |""".stripMargin
    } else {
      s"""[${attrType}]Supported data types:
         | ${typeExplain[Except]}
         | """.stripMargin
    }
  }

  def typeExplain[T <: AnyKind](using tpe: Type[T])(using quotes: Quotes): String = {
    import quotes.reflect.*

    val typeRepr = TypeRepr.of[T]

    @tailrec
    def rec(trem: List[TypeRepr], buf: List[TypeRepr]): List[TypeRepr] = {
      trem match
        case OrType(t1, t2) :: tail => rec(t1 :: t2 :: tail, buf)
        case head :: tail           => rec(tail, head :: buf)
        case Nil                    => buf
    }

    rec(typeRepr :: Nil, Nil).map(x => "  - " + formatType(using x.asType)).sortBy(_.length).mkString("\n")
  }

  def exprShow[T: Type](expr: Expr[T])(using quotes: Quotes): String = {

    def constTextValue(expr: Expr[T])(using quotes: Quotes): Option[String] = {
      import quotes.reflect.*

      import quoted.*

      @tailrec
      def rec(trem: Term): Option[String] = {
        trem match
          case Apply(
                Select(New(textType), _),
                List(Literal(StringConstant(value))),
              ) if textType.tpe <:< TypeRepr.of[scala.xml.Text] =>
            Some(value)
          case Literal(StringConstant(value)) => Some(value)
          case Typed(e, _)                    => rec(e)
          case Block(Nil, e)                  => rec(e)
          case Inlined(_, Nil, e)             => rec(e)
          case _                              => None
      }

      rec(expr.asTerm)
    }

    constTextValue(expr).map(x => "\"" + x + "\"").getOrElse(expr.show)
  }

  def formatType[T <: AnyKind](using Type[T])(using quotes: Quotes): String = {
    import quotes.*
    import quotes.reflect.*

    given printer: Printer[TypeRepr] = new Printer[TypeRepr] { self =>
      val scalaFuncs: List[TypeRepr] = (0 to 22)
        .map(x => s"scala.Function${x}")
        .map(fullName => Symbol.classSymbol(fullName))
        .map(symbol => TypeTree.ref(symbol).tpe)
        .toList
      val defaultPrinter             = Printer.TypeReprCode
      given _self: Printer[TypeRepr] = self

      def show(t: TypeRepr): String = {
        t match {
          case AppliedType(tycon, args) if scalaFuncs.exists(fType => fType =:= tycon) => {
            if args.length == 1 then {
              s"() => ${args.last.show}"
            } else {
              s"(${args.dropRight(1).map(show).mkString(", ")}) => ${args.last.show}"
            }
          }
          case AppliedType(tycon, args)                                                => {
            s"""${tycon.show}[${args.map(show).mkString(", ")}]"""
          }
          case text
              if (text <:< TypeRepr.of[Text] || text <:< TypeRepr.of[String])
                && !(text =:= TypeRepr.of[Nothing]) =>
            "String"

          case AndType(left, right) => s"(${show(left)}) & (${show(right)})"
          case OrType(left, right)  => s"(${show(left)}) | (${show(right)})"

          case TypeBounds(low, hi) if low =:= TypeRepr.of[Nothing] => s"_ <: ${show(hi)}"
          case TypeBounds(low, hi) if hi =:= TypeRepr.of[Any]      => s"_ >: ${show(low)}"

          case TypeBounds(low, hi) => s"_ >: ${show(low)} <: ${show(hi)}"
          case other               => {
            val res = defaultPrinter.show(other)
            if res.startsWith("scala.") && res.count(_ == '.') == 1 then res.drop("scala.".length)
            else if res.startsWith("scala.collection.immutable.") && res.count(_ == '.') == 3 then
              res.drop("scala.collection.immutable.".length)
            else if res.startsWith("scala.scalajs.js.") then res.drop("scala.scalajs.".length)
            else if res.startsWith("org.scalajs.dom.") then res.drop("org.scalajs.".length)
            else res
          }
        }
      }
    }

    TypeRepr.of[T].show(using printer)
  }

  inline def getTypeString[T]: String = ${ getTypeStringMacros[T] }

  def getTypeStringMacros[T: Type](using quotes: Quotes): Expr[String] = Expr(formatType[T])

  def unsupportConstProp[T <: AnyKind](value: String | scala.Null)(using tpe: Type[T])(using
    quotes: Quotes,
    position: MacrosPosition,
    attrType: AttrType,
  ): Nothing =
    raiseError {
      import quotes.reflect.*

      val tpeInfo =
        if (TypeTree.of(using tpe).tpe =:= TypeTree.of[Boolean].tpe) then "Boolean | true | false"
        else formatType(using tpe)
      if isChinese then {
        s"""该属性要求类型: ${tpeInfo}, 实际值为: "${value}" """.stripMargin
      } else {
        s"""This property requires a type: ${tpeInfo}, actual value is: "${value}" """.stripMargin
      }
    }

  def raiseError(msg: String)(using
    quotes: Quotes,
    position: MacrosPosition,
    attrType: AttrType,
  ): Nothing = {
    import quotes.reflect.*
    report.errorAndAbort(s"[${attrType}]" + msg + s"\n${position}")
  }

  def logInfo(msg: String)(using quotes: Quotes, position: MacrosPosition): Unit = {
    import quotes.reflect.*
    if ShowTypeHints then report.info(msg + s"\n${position}")
  }

  case class AttrType(str: String) extends AnyVal {
    override def toString: String = str
  }

  object AttrType {
    given default: AttrType = AttrType("Info")
  }
}
