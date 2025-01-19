package scala.xml

import com.raquo.ew.ewArray

import java.util.Locale
import scala.annotation.tailrec
import scala.quoted.*
import scala.scalajs.js.JSStringOps.*
import scala.util.Try as ScalaTry
import scala.xml.binders.*

object MacrosTool {

  /** NodeBuffer添加Text节点时候执行:{{{Text(value) => val a =
    * value.trim; if(a.noEmpty) add(a) }}}
    */
  inline def trimOrDropTextNode(inline node: Text, underlying: NodeBuffer): NodeBuffer = {
    ${ trimOrDropTextNodeMacros('node, 'underlying) }
  }

  private def trimOrDropTextNodeMacros(
    nodeExpr: Expr[Text],
    underlying: Expr[NodeBuffer],
  )(using quotes: Quotes): Expr[NodeBuffer] = {
    import quotes.reflect.*

    import quoted.*
    constTextValue(nodeExpr) match
      case Some(value) =>
        // 检查是否是Text节点，并且有一个字符串字面量作为参数
        val trimmedValue = value.trim
        if trimmedValue.isEmpty then underlying // 空Text不添加
        else '{ $underlying.&+(${ Expr(trimmedValue) }) }
      case None        =>
        report.info("This is not a const TextValue")
        '{ $underlying.&+($nodeExpr.data) }
  }

  // 常量提取
  /** 提取字符串常量或者Text常量 */
  private def constTextValue[T](expr: Expr[T])(using quotes: Quotes): Option[String] = {
    import quotes.reflect.*

    import quoted.*

    val textTypeTpe                     = TypeTree.of[scala.xml.Text].tpe
    @tailrec
    def rec(trem: Term): Option[String] = {
      trem match
        case Apply(
              Select(New(textType), _),
              List(Literal(StringConstant(value))),
            ) if textType.tpe =:= textTypeTpe =>
          Some(value)
        case Literal(StringConstant(value)) => Some(value)
        case Typed(e, _)                    => rec(e)
        case Block(Nil, e)                  => rec(e)
        case Inlined(_, Nil, e)             => rec(e)
        case _                              => None
    }
    rec(expr.asTerm)
  }

  private def constAnyValue[T](expr: Expr[T])(using quotes: Quotes): Option[String | Null] = {
    import quotes.reflect.*

    import quoted.*

    val textTypeTpe                            = TypeTree.of[scala.xml.Text].tpe
    @tailrec
    def rec(trem: Term): Option[String | Null] = {
      trem match
        case Apply(
              Select(New(textType), _),
              List(Literal(StringConstant(value))),
            ) if textType.tpe =:= textTypeTpe =>
          Some(value)
        case Literal(value: Constant) =>
          val constValue = value.value
          if constValue == null then Some(null)
          else Some(constValue.toString)
        case Typed(e, _)              => rec(e)
        case Block(Nil, e)            => rec(e)
        case Inlined(_, Nil, e)       => rec(e)
        case _                        => None
    }
    rec(expr.asTerm)
  }

  private def typeEquals[Base: Type, Target: Type](using quotes: Quotes): Boolean = {
    import quotes.reflect.*
    TypeTree.of[Base].tpe =:= TypeTree.of[Target].tpe
  }

  private def typeInOptionEquals[Base: Type, Target: Type](using quotes: Quotes): Boolean = {
    import quotes.reflect.*
    TypeTree.of[Base].tpe =:= TypeTree.of[Option[Target]].tpe
  }

  // 属性绑定
  inline def attribute[T](inline x: UnprefixedAttribute[T]): MetaData = ${
    unprefixedattributeMacro('x)
  }

  inline def attributeRx[CC[x] <: Source[x], V](inline x: UnprefixedAttribute[CC[V]]): MetaData =
    ???
//    Null
//    ${
//      unprefixedattributeMacro('x)
//    }

  inline def attribute[T](inline x: PrefixedAttribute[T]): MetaData = ${ prefixedAttributeMacro('x) }

  inline def attributeRx[CC[x] <: Source[x], V](inline x: PrefixedAttribute[CC[V]]): MetaData =
    ???
//    Null
//    ${
//      prefixedAttributeMacro('x)
//    }

  def unprefixedattributeMacro[T: Type](
    attr: Expr[UnprefixedAttribute[T]],
  )(using quotes: Quotes): Expr[MetaData] = {
    import quotes.*
    import quotes.reflect.*

    // 这里应该是安全的
    val '{ new UnprefixedAttribute(${ keyExpr }, ${ valueExpr: Expr[T] }, ${ nextExpr }) } = attr: @unchecked
    val Literal(StringConstant(attrKey: String))                                           = keyExpr.asTerm: @unchecked

    val binderExpr: Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = attrKey match {
      case Events(eventKey) =>
        Expr
          .summon[ToJsListener[T]]
          .map(listener => '{ new AddEventListener(${ Expr(eventKey) }, ${ valueExpr }, ${ listener }) })
          .getOrElse(CompileMessage.unsupportEventType[T])
      case Props(propKey)   =>
        constAnyValue(valueExpr) match
          case Some(const) =>
            def htmlConstPropBinder[R: Type: ToExpr](
              f: String => R,
            ): Expr[(NamespaceBinding, ReactiveElementBase) => Unit] =
              const match {
                case null        => '{ PropsApi.removeHtmlPropertyBinder(${ Expr(propKey) }) }
                case constString =>
                  ScalaTry(f(constString))
                    .map(r => '{ PropsApi.setHtmlPropertyBinder(${ Expr(propKey) }, ${ Expr(r) }) })
                    .getOrElse(CompileMessage.unsupportConstProp[R](const))
              }

            propKey match {
              case Props.StringProp(_) => htmlConstPropBinder(_.toString)
              case Props.BoolProp(_)   => htmlConstPropBinder(_.toBoolean)
              case Props.DoubleProp(_) => htmlConstPropBinder(_.toDouble)
              case Props.IntProp(_)    => htmlConstPropBinder(_.toInt)
            }
          case None        =>
            def htmlPropBinder[EXPECT: Type: ToExpr]: Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = {
              valueExpr match {
                case strExpr: Expr[EXPECT] @unchecked if typeEquals[T, EXPECT]                =>
                  '{ PropsApi.setHtmlPropertyBinder(${ Expr(propKey) }, ${ strExpr }) }
                case optStr: Expr[Option[EXPECT]] @unchecked if typeInOptionEquals[T, EXPECT] =>
                  '{
                    ${ optStr }.fold(MetaData.EmptyBinder)(str =>
                      PropsApi.setHtmlPropertyBinder(
                        ${ Expr(propKey) }, {
                          str
                        }))
                  }

                case _ => CompileMessage.expectationType[T, EXPECT | Option[EXPECT]]
              }
            }

            propKey match {
              case Props.StringProp(_) => htmlPropBinder[String]
              case Props.BoolProp(_)   => htmlPropBinder[Boolean]
              case Props.DoubleProp(_) => htmlPropBinder[Double]
              case Props.IntProp(_)    => htmlPropBinder[Int]
//              case _                   => CompileMessage.impossibleError(attr)
            }

      case Attrs(attrKey) if Attrs.isComposite(attrKey) =>
        val separator = " "
        val constList = constAnyValue(valueExpr).map(x =>
          if x == null || x.isEmpty then Nil
          else x.split(separator).filter(_.nonEmpty).toList)
        constList match {
          case Some(items) => '{ AttrsApi.setCompositeAttributeBinder(${ Expr(attrKey) }, ${ Expr(items) }, Nil) }
          case None        =>
            valueExpr match {
              case strExpr: Expr[String] @unchecked if typeEquals[T, String]                =>
                '{
                  AttrsApi.setCompositeAttributeBinder(${ Expr(attrKey) }, AttrsApi.normalize(${ strExpr }, " "), Nil)
                }
              case optStr: Expr[Option[String]] @unchecked if typeInOptionEquals[T, String] =>
                '{
                  AttrsApi.setCompositeAttributeBinder(
                    ${ Expr(attrKey) },
                    ${ optStr }.map(item => AttrsApi.normalize(item, " ")).getOrElse(Nil),
                    Nil)
                }
              case seqExpr: Expr[List[String]] @unchecked if typeEquals[T, List[String]]    =>
                '{
                  AttrsApi.setCompositeAttributeBinder(
                    ${ Expr(attrKey) },
                    ${ seqExpr }.flatMap(item => AttrsApi.normalize(item, " ")),
                    Nil)
                }
              case _                                                                        =>
                CompileMessage.expectationType[T, String | Option[String] | List[String]]
            }
        }

      case Attrs(attrKey) if !Attrs.isComposite(attrKey) =>
        constAnyValue(valueExpr) match {
          case Some(null)          => '{ AttrsApi.removeHtmlAttributeBinder(${ Expr(attrKey) }) }
          case Some(const: String) => '{ AttrsApi.setHtmlAttributeBinder(${ Expr(attrKey) }, ${ Expr(const) }) }
          case None                =>
            valueExpr match {
              case strExpr: Expr[String] @unchecked if typeEquals[T, String]                =>
                '{ AttrsApi.setHtmlAttributeBinder(${ Expr(attrKey) }, ${ strExpr }) }
              case optStr: Expr[Option[String]] @unchecked if typeInOptionEquals[T, String] =>
                '{
                  ${ optStr }
                    .map(strExpr => AttrsApi.setHtmlAttributeBinder(${ Expr(attrKey) }, strExpr))
                    .getOrElse(MetaData.EmptyBinder)
                }
              case _                                                                        =>
                CompileMessage.expectationType[T, String | Option[String]]
            }
        }
      case _                                             =>
        println("-----------|Unknown>" + attr.show)
        CompileMessage.impossibleError(valueExpr)
    }

    '{ MetaData(${ binderExpr }, ${ nextExpr }) }
  }

  def prefixedAttributeMacro[T: Type](
    attr: Expr[PrefixedAttribute[T]],
  )(using quotes: Quotes): Expr[MetaData] = {
    import quotes.*
    import quotes.reflect.*
    // 这里应该是安全的
    val '{ new PrefixedAttribute(${ prefixExpr }, ${ keyExpr }, ${ valueExpr }, ${ nextExpr }) } = attr: @unchecked
    val Literal(StringConstant(attrPrefix: String))                                              = prefixExpr.asTerm: @unchecked
    val Literal(StringConstant(attrKey: String))                                                 = keyExpr.asTerm: @unchecked

    // 如果我可以获取默认配置获取的namespaceURI,那么就不需要通过Elem输入了
    val namespaceURI = TopScope.namespaceURI(attrPrefix)
    namespaceURI match {
      case Some(namespace) => ???
      case None            => ???
    }
    CompileMessage.impossibleError(attr)
  }

  /** 编译异常提示 */
  object CompileMessage {

    val isChinese =
      Seq(Locale.CHINESE, Locale.CHINA, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE).contains(
        Locale.getDefault)

    def unsupportEventType[T: Type](using quotes: Quotes): Nothing = raiseError {
      if isChinese then {
        s"""不支持的事件类型 ${Type.show[T]}, 受到支持事件函数:
             |  - () => Unit
             |  - (event:T <: dom.Event) => Unit
             |  - (value:String) => Unit
             |  - (checked:Boolean) => Unit
             |  - (file:List[dom.File]) => Unit
             |""".stripMargin
      } else {
        s"""Unsupport Events Type ${Type.show[T]}, Supported event functions:
             |  - () => Unit
             |  - (event:T <: dom.Event) => Unit
             |  - (value:String) => Unit
             |  - (checked:Boolean) => Unit
             |  - (file:List[dom.File]) => Unit
             |""".stripMargin
      }
    }

    def impossibleError[T: Type](expr: Expr[T])(using quotes: Quotes): Nothing = raiseError {
      if isChinese then s"不可能的代码分支 ${Type.show[T]}, ${expr.show}"
      else s"Impossible code branch ${Type.show[T]}, ${expr.show}"
    }

    def impossibleError[T: Type](using quotes: Quotes): Nothing = raiseError {
      if isChinese then s"不可能的代码分支 ${Type.show[T]}" else s"Impossible code branch ${Type.show[T]}"
    }

    def expectationType[T: Type, Except: Type](using quotes: Quotes): Nothing = raiseError {
      if isChinese then {
        s"""期望类型为 `${Type.show[Except]}`, 但是实际类型为 `${Type.show[T]}`""".stripMargin
      } else {
        s"""Expectation Type is `${Type.show[Except]}`, But the actual type is `${Type.show[T]}`""".stripMargin
      }
    }

    def unsupportConstProp[T: Type](value: String)(using quotes: Quotes): Nothing = raiseError {
      import quotes.reflect.*

      val tpeInfo = if (TypeTree.of[T].tpe =:= TypeTree.of[Boolean].tpe) then "Boolean | true | false" else Type.show[T]
      if isChinese then {
        s"""该属性要求类型: ${tpeInfo}, 实际值为: ${value}""".stripMargin
      } else {
        s"""This property requires a type: ${tpeInfo}, actual value is: ${value}""".stripMargin
      }
    }

    def raiseError(msg: String)(using quotes: Quotes): Nothing = {
      import quotes.reflect.*
      report.errorAndAbort(msg)
    }
  }

}
