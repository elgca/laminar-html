package scala.xml

import com.raquo.laminar.lifecycle.MountContext
import sourcecode.{File, FullName, Line}

import java.util.Locale
import scala.annotation.tailrec
import scala.quoted.*
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
    import quoted.*
    constTextValue(nodeExpr) match
      case Some(value) =>
        // 检查是否是Text节点，并且有一个字符串字面量作为参数
        val trimmedValue = value.trim
        if trimmedValue.isEmpty then underlying // 空Text不添加
        else '{ ${ underlying }.&+(${ Expr(trimmedValue) }) }
      case None        =>
        '{ ${ underlying }.&+(${ nodeExpr }.data) }
  }

  // 常量提取
  /** 提取字符串常量或者Text常量 */
  private def constTextValue[T](expr: Expr[T])(using quotes: Quotes): Option[String] = {
    import quotes.reflect.*

    import quoted.*

    val textTypeTpe = TypeTree.of[scala.xml.Text].tpe

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

  private def constAnyValue[T](expr: Expr[T])(using quotes: Quotes): Option[Option[String]] = {
    import quotes.reflect.*

    import quoted.*

    val textTypeTpe = TypeTree.of[scala.xml.Text].tpe
    val NullConst   = Some(None)

    @tailrec
    def rec(trem: Term): Option[Option[String]] = {
      trem match
        case Apply(
              Select(New(textType), _),
              List(Literal(StringConstant(value))),
            ) if textType.tpe =:= textTypeTpe =>
          Some(Some(value))
        case Literal(UnitConstant())       => NullConst
        case Literal(NullConstant())       => NullConst
        case Literal(ClassOfConstant(tpe)) =>
          CompileMessage.unsupportConstPropBase(tpe.toString, tpe.asType)
        case Literal(value: Constant)      =>
          val constValue = value.value
          Some(Option(constValue).map(_.toString))
        case Ident("Nil")                  => NullConst // 处理一种很奇怪的场景, 当为 <div propkey="" />时候,常量为Nil
        case Typed(e, _)                   => rec(e)
        case Block(Nil, e)                 => rec(e)
        case Inlined(_, Nil, e)            => rec(e)
        case _                             => None
    }
    rec(expr.asTerm)
  }

  private def typeEquals[Base: Type, Target: Type](using quotes: Quotes): Boolean = {
    import quotes.reflect.*
    TypeTree.of[Base].tpe <:< TypeTree.of[Target].tpe
  }

  private def typeInOptionEquals[Base: Type, Target: Type](using quotes: Quotes): Boolean = {
    import quotes.reflect.*
    TypeTree.of[Base].tpe <:< TypeTree.of[Option[Target]].tpe
  }

  // 属性绑定
  inline def attribute[T](inline x: UnprefixedAttribute[T]): MetaData = ${
    unprefixedattributeMacro('x)
  }

  inline def attribute[T](inline x: PrefixedAttribute[T]): MetaData = ${ prefixedAttributeMacro('x) }

  inline def attributeRx[CC[x] <: Source[x], V](inline x: UnprefixedAttribute[CC[V]]): MetaData =
    ${ unprefixedattributeRxMacro('x) }

  inline def attributeRx[CC[x] <: Source[x], V](inline x: PrefixedAttribute[CC[V]]): MetaData =
    ${ prefixedAttributeRxMacro('x) }

  def bindEvent[T: Type](
    eventKey: String,
    addListenerFunction: Expr[T],
    removeListenerFunction: Expr[Option[T]],
  )(using quotes: Quotes): Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = {
    import EventsApi.*
    val conversion: Option[Expr[ToJsListener[T]]] = Expr.summon[ToJsListener[T]]
    conversion match
      case Some(funConversion) =>
        '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
          {
            ${ removeListenerFunction }.foreach(f =>
              element.ref.removeEventListener(${ Expr(eventKey) }, ${ funConversion }.apply(f)))
            element.ref.addEventListener(${ Expr(eventKey) }, ${ funConversion }.apply(${ addListenerFunction }))
          }
        }
      case None                => CompileMessage.unsupportEventType[T]
  }

  def compositeItem[T: Type](valueExpr: Expr[T])(using quotes: Quotes): Expr[List[String]] = {
    val separator = " "
    val constStr  = constAnyValue(valueExpr)
    if constStr.isDefined then {
      val items = constStr.flatten
        .filter(_.nonEmpty)
        .map(_.split(separator).filter(_.nonEmpty).toList)
        .getOrElse(List.empty)
      Expr(items)
    } else {
      valueExpr match {
        case strExpr: Expr[String] @unchecked if typeEquals[T, String]                =>
          '{ AttrsApi.normalize(${ strExpr }, " ") }
        case optStr: Expr[Option[String]] @unchecked if typeEquals[T, Option[String]] =>
          '{ ${ optStr }.map(item => AttrsApi.normalize(item, " ")).getOrElse(Nil) }
        case seqExpr: Expr[List[String]] @unchecked if typeEquals[T, List[String]]    =>
          '{ ${ seqExpr }.flatMap(item => AttrsApi.normalize(item, " ")) }
        case _                                                                        =>
          CompileMessage.expectationType[T, String | Option[String] | List[String]]
      }
    }
  }

  def bindCompositeAttribute(
    namespaceURI: Option[String],
    prefix: Option[String],
    attrKey: String,
    itemsToAdd: Expr[List[String]],
    itemsToRemove: Expr[List[String]],
  )(using quotes: Quotes): Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = {
    val name = prefix.map(_ + ":" + attrKey).getOrElse(attrKey)

    val nsFunc: Expr[NamespaceBinding => Option[String]] = prefix
      .map(px => {
        if namespaceURI.isDefined then '{ (ns: NamespaceBinding) => ${ Expr(namespaceURI) } }
        else '{ (ns: NamespaceBinding) => ns.namespaceURI(${ Expr(px) }) }
      })
      .getOrElse('{ (ns: NamespaceBinding) => None })

    '{ AttrsApi.setCompositeAttributeBinder(${ nsFunc }, ${ Expr(name) }, ${ itemsToAdd }, ${ itemsToRemove }) }
  }

  def bindAttribute[T: Type](
    namespaceURI: Option[String],
    prefix: Option[String],
    attrKey: String,
    valueExpr: Expr[T],
  )(using quotes: Quotes): Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = {
    val constStr: Option[String | Null] = constAnyValue(valueExpr).map(_.orNull)
    val attrValue: Expr[String | Null]  = constStr match
      case Some(const) => Expr(const)
      case None        =>
        valueExpr match {
          case strExpr: Expr[String] @unchecked if typeEquals[T, String]                => strExpr
          case optStr: Expr[Option[String]] @unchecked if typeEquals[T, Option[String]] => '{ ${ optStr }.orNull }
          case _                                                                        => CompileMessage.expectationType[T, String | Option[String]]
        }

    val nsFunc: Expr[NamespaceBinding => Option[String]] = prefix
      .map(px => {
        if namespaceURI.isDefined then '{ (ns: NamespaceBinding) => ${ Expr(namespaceURI) } }
        else '{ (ns: NamespaceBinding) => ns.namespaceURI(${ Expr(px) }) }
      })
      .getOrElse('{ (ns: NamespaceBinding) => None })

    val name = prefix.map(_ + ":" + attrKey).getOrElse(attrKey)

    '{ AttrsApi.setHtmlAttributeBinder(${ nsFunc }, ${ Expr(name) }, ${ attrValue }) }
  }

  def bindHtmlProp[T: Type](
    propKey: String,
    valueExpr: Expr[T],
  )(using quotes: Quotes): Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = {
    constAnyValue(valueExpr) match
      case Some(const) =>
        def htmlConstPropBinder[R: Type: ToExpr](
          f: String => R,
        ): Expr[(NamespaceBinding, ReactiveElementBase) => Unit] =
          const match {
            case None              => '{ PropsApi.removeHtmlPropertyBinder(${ Expr(propKey) }) }
            case Some(constString) =>
              ScalaTry(f(constString))
                .map(r => '{ PropsApi.setHtmlPropertyBinder(${ Expr(propKey) }, ${ Expr(r) }) })
                .getOrElse(CompileMessage.unsupportConstProp[R](constString))
          }

        propKey match {
          case Props.StringProp() => htmlConstPropBinder(_.toString)
          case Props.BoolProp()   => htmlConstPropBinder(_.toBoolean)
          case Props.DoubleProp() => htmlConstPropBinder(_.toDouble)
          case Props.IntProp()    => htmlConstPropBinder(_.toInt)
        }
      case None        =>
        def htmlPropBinder[EXPECT: Type: ToExpr]: Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = {
          valueExpr match {
            case strExpr: Expr[EXPECT] @unchecked if typeEquals[T, EXPECT]                =>
              '{ PropsApi.setHtmlPropertyBinder(${ Expr(propKey) }, ${ strExpr }) }
            case optStr: Expr[Option[EXPECT]] @unchecked if typeEquals[T, Option[EXPECT]] =>
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
          case Props.StringProp() => htmlPropBinder[String]
          case Props.BoolProp()   => htmlPropBinder[Boolean]
          case Props.DoubleProp() => htmlPropBinder[Double]
          case Props.IntProp()    => htmlPropBinder[Int]
        }
  }

  def dattributeMacro[T: Type](
    namespaceURI: Option[String],
    prefix: Option[String],
    key: String,
    valueExpr: Expr[T],
  )(using quotes: Quotes): Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = {
    import quotes.*
    import quotes.reflect.*

    // 这里尝试获取隐式 AttributeBinder["hello", T] , hello是key的常量类型
    val keyConstType       = ConstantType(StringConstant(key))
    val binderSymbol       = Symbol.classSymbol(classOf[AttributeBinder[?, ?]].getName)
    val clsTpe             = TypeTree.ref(binderSymbol).tpe
    val binderTpe: Type[?] = AppliedType(clsTpe, List(keyConstType, TypeRepr.of[T])).asType

    Implicits.search(TypeRepr.of(using binderTpe)) match {
      case iss: ImplicitSearchSuccess => {
        val binderExpr: Expr[AttributeBinder[String, T]] =
          iss.tree.asExpr.asInstanceOf[Expr[AttributeBinder[String, T]]]

        return '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
          {
            ${ binderExpr }.bindAttr(
              element,
              None,
              None,
              ${ Expr(key) },
              ${ valueExpr },
            )
          }
        }
      }
      case isf: ImplicitSearchFailure =>
    }

    // 在xhtml中嵌入lamianr的Modifier
    Expr.summon[AttributeBinder[String, T]] match {
      case Some(binder) =>
        return '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
          {
            ${ binder }.bindAttr(
              element,
              None,
              None,
              ${ Expr(key) },
              ${ valueExpr },
            )
          }
        }
      case None         =>
    }

    key match {
      case Hooks(hookKey) if prefix.isEmpty   =>
        import HooksApi.*
        hookKey match
          case "onmount"   =>
            val conversion: Option[Expr[ToMountFunc[T]]] = Expr.summon[ToMountFunc[T]]
            val callbackFunc                             = conversion
              .map(x => '{ ${ x }.apply(${ valueExpr }) })
              .getOrElse(CompileMessage.expectationType[T, MountFuncValid])

            '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
              L.onMountCallback(${ callbackFunc }).apply(element)
            }
          case "onunmount" => {
            val conversion: Option[Expr[ToUnMountFunc[T]]] = Expr.summon[ToUnMountFunc[T]]
            val callbackFunc                               = conversion
              .map(x => '{ ${ x }.apply(${ valueExpr }) })
              .getOrElse(CompileMessage.expectationType[T, UnMountFuncValid])
            '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
              L.onUnmountCallback(${ callbackFunc }).apply(element)
            }
          }
      case Events(eventKey) if prefix.isEmpty => bindEvent(eventKey, valueExpr, Expr(None))

      case Props(propKey) if prefix.isEmpty =>
        bindHtmlProp(
          propKey = propKey,
          valueExpr = valueExpr,
        )

      case Attrs(attrKey) if Attrs.isComposite(attrKey) =>
        val addItems = compositeItem(valueExpr = valueExpr)
        bindCompositeAttribute(
          namespaceURI = namespaceURI,
          prefix = prefix,
          attrKey = attrKey,
          itemsToAdd = addItems,
          itemsToRemove = Expr(Nil),
        )

      case Attrs(attrKey) if !Attrs.isComposite(attrKey) =>
        bindAttribute(
          namespaceURI = namespaceURI,
          prefix = prefix,
          attrKey = attrKey,
          valueExpr = valueExpr,
        )

      case _ =>
        CompileMessage.notDefineAttrKey(key, valueExpr)
        bindAttribute(
          namespaceURI = namespaceURI,
          prefix = prefix,
          attrKey = key,
          valueExpr = valueExpr,
        )
    }
  }

  def unprefixedattributeMacro[T: Type](
    attr: Expr[UnprefixedAttribute[T]],
  )(using quotes: Quotes): Expr[MetaData] = {
    import quotes.*
    import quotes.reflect.*

    // 这里应该是安全的
    val '{ new UnprefixedAttribute(${ keyExpr }, ${ valueExpr: Expr[T] }, ${ nextExpr }) } = attr: @unchecked

    val Literal(StringConstant(attrKey: String)) = keyExpr.asTerm: @unchecked
    val prefix: Option[String]                   = None
    val namespaceURI: Option[String]             = None

    val binderExpr: Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = dattributeMacro(
      namespaceURI = namespaceURI,
      prefix = prefix,
      key = attrKey,
      valueExpr = valueExpr,
    )

    '{ MetaData(${ binderExpr }, ${ nextExpr }) }
  }

  def prefixedAttributeMacro[T: Type](
    attr: Expr[PrefixedAttribute[T]],
  )(using quotes: Quotes): Expr[MetaData] = {
    import quotes.*
    import quotes.reflect.*
    // 这里应该是安全的
    val '{ new PrefixedAttribute(${ prefixExpr }, ${ keyExpr }, ${ valueExpr }, ${ nextExpr }) } = attr: @unchecked

    val Literal(StringConstant(attrPrefix: String)) = prefixExpr.asTerm: @unchecked
    val Literal(StringConstant(attrKey: String))    = keyExpr.asTerm: @unchecked

    // 如果我可以获取默认配置获取的namespaceURI,那么就不需要通过Elem输入了
    val namespaceURI                                                      = TopScope.namespaceURI(attrPrefix)
    val binderExpr: Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = dattributeMacro(
      namespaceURI = namespaceURI,
      prefix = Some(attrPrefix),
      key = attrKey,
      valueExpr = valueExpr,
    )

    '{ MetaData(${ binderExpr }, ${ nextExpr }) }
  }

  // Rx变量解析
  def unprefixedattributeRxMacro[V: Type, CC <: Source[V]: Type](
    attr: Expr[UnprefixedAttribute[CC]],
  )(using quotes: Quotes): Expr[MetaData] = {
    import quotes.*
    import quotes.reflect.*

    // 这里应该是安全的
    val '{ new UnprefixedAttribute(${ keyExpr }, ${ valueExpr: Expr[CC] }, ${ nextExpr }) } = attr: @unchecked

    val Literal(StringConstant(attrKey: String)) = keyExpr.asTerm: @unchecked
    val prefix: Option[String]                   = None
    val namespaceURI: Option[String]             = None
    val sourceValue: Expr[CC]                    = valueExpr

    val binderExpr: Expr[(NamespaceBinding, ReactiveElementBase) => Unit] =
      if attrKey == "value" && prefix.isEmpty then {
        sourceValue match {
          case sourceStr: Expr[Source[String]] @unchecked if typeEquals[V, String] =>
            '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
              ReactiveElement.bindFn(element, ${ sourceStr }.toObservable) { nextValue =>
                PropsApi.updateWhenKeyIsValue(element, nextValue)
              }
            }

          case _ => CompileMessage.expectationType[CC, Source[String]]
        }
      } else {
        attrKey match {
          case Events(eventKey) => {
            '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
              var before: Option[V] = None
              ReactiveElement.bindFn(element, ${ sourceValue }.toObservable) { nextValue =>
                ${
                  bindEvent(eventKey, 'nextValue, 'before)
                }.apply(ns, element)
                before = Some(nextValue)
              }
            }
          }
          case key              => {
            '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
              ReactiveElement.bindFn(element, ${ sourceValue }.toObservable) { nextValue =>
                ${
                  val updaterExpr: Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = dattributeMacro(
                    namespaceURI = namespaceURI,
                    prefix = prefix,
                    key = key,
                    valueExpr = 'nextValue,
                  )
                  updaterExpr
                }.apply(ns, element)
              }
            }
          }
        }
      }

    '{ MetaData(${ binderExpr }, ${ nextExpr }) }
  }

  def prefixedAttributeRxMacro[V: Type, CC <: Source[V]: Type](
    attr: Expr[PrefixedAttribute[CC]],
  )(using
    quotes: Quotes,
  ): Expr[MetaData] = {
    import quotes.*
    import quotes.reflect.*

    // 这里应该是安全的
    val '{
      new PrefixedAttribute(${ prefixExpr }, ${ keyExpr }, ${ valueExpr: Expr[CC] }, ${ nextExpr })
    } = attr: @unchecked

    val Literal(StringConstant(attrPrefix: String)) = prefixExpr.asTerm: @unchecked
    val Literal(StringConstant(attrKey: String))    = keyExpr.asTerm: @unchecked

    // 如果我可以获取默认配置获取的namespaceURI,那么就不需要通过Elem输入了
    val namespaceURI          = TopScope.namespaceURI(attrPrefix)
    val sourceValue: Expr[CC] = valueExpr

    val binderExpr: Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = '{
      (ns: NamespaceBinding, element: ReactiveElementBase) =>
        ReactiveElement.bindFn(element, ${ sourceValue }.toObservable) { nextValue =>
          ${
            val updaterExpr: Expr[(NamespaceBinding, ReactiveElementBase) => Unit] = dattributeMacro(
              namespaceURI = namespaceURI,
              prefix = Option(attrPrefix),
              key = attrKey,
              valueExpr = 'nextValue,
            )
            updaterExpr
          }.apply(ns, element)
        }
    }

    '{ MetaData(${ binderExpr }, ${ nextExpr }) }
  }

  /** 编译异常提示 */
  object CompileMessage {

    val isChinese =
      Seq(Locale.CHINESE, Locale.CHINA, Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE).contains(
        Locale.getDefault)

    def unsupportEventType[T: Type](using quotes: Quotes, position: MacrosPosition): Nothing = raiseError {
      if isChinese then {
        s"""不支持的事件类型 ${formatType[T]}, 受到支持事件函数:
             |  - () => Unit
             |  - (event:T <: dom.Event) => Unit
             |  - (value:String) => Unit
             |    等效于 (e: dom.Event) => f(e.target.value.getOrElse(""))
             |  - (checked:Boolean) => Unit
             |    等效于 (e: dom.Event) => f(e.target.checked.getOrElse(false))
             |  - (file:List[dom.File]) => Unit
             |    等效于 (e: dom.Event) => f(e.target.files.getOrElse(List.empty))
             |""".stripMargin
      } else {
        s"""Unsupport Events Type ${formatType[T]}, Supported event functions:
             |  - () => Unit
             |  - (event:T <: dom.Event) => Unit
             |  - (value:String) => Unit
             |    Equivalent (e: dom.Event) => f(e.target.value.getOrElse(""))
             |  - (checked:Boolean) => Unit
             |    Equivalent (e: dom.Event) => f(e.target.checked.getOrElse(false))
             |  - (file:List[dom.File]) => Unit
             |    Equivalent (e: dom.Event) => f(e.target.files.getOrElse(List.empty))
             |""".stripMargin
      }
    }

    def notDefineAttrKey[T: Type](key: String, expr: Expr[T])(using quotes: Quotes, position: MacrosPosition): Unit = {
      logInfo {
        if isChinese then s"""未定义的属性 ${key} = ${expr.show}, 类型: ${formatType[T]}"""
        else s"""Not Define Attribute Key ${key} = ${expr.show}, type: ${formatType[T]}"""
      }
    }

    def ????(using quotes: Quotes, position: MacrosPosition): Nothing = raiseError {
      if isChinese then s"还未实现功能/非法分支"
      else s"an implementation is missing"
    }

    def expectationType[T: Type, Except: Type](using quotes: Quotes, position: MacrosPosition): Nothing = raiseError {
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

    def typeExplain[T: Type](using quotes: Quotes): String = {
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

    def formatType[T <: AnyKind](using Type[T])(using quotes: Quotes): String = {
      import quotes.*
      import quotes.reflect.*
      val typeRepr = TypeRepr.of[T]

      val scalaFuncs: List[TypeRepr] = (0 to 22)
        .map(x => s"scala.Function${x}")
        .map(fullName => Symbol.classSymbol(fullName))
        .map(symbol => TypeTree.ref(symbol).tpe)
        .toList

      typeRepr match
        case AppliedType(tycon, args) if scalaFuncs.exists(fType => fType =:= tycon) =>
          if args.length == 1 then {
            s"() => ${args.last.show}"
          } else {
            s"(${args.dropRight(1).map(x => formatType(using x.asType)).mkString(", ")}) => ${args.last.show}"
          }
        case other                                                                   => other.show
    }

    def unsupportConstProp[T: Type](value: String)(using quotes: Quotes, position: MacrosPosition): Nothing =
      unsupportConstPropBase(value, summon[Type[T]])

    def unsupportConstPropBase(value: String, tpe: Type[?])(using quotes: Quotes, position: MacrosPosition): Nothing =
      raiseError {
        import quotes.reflect.*

        val tpeInfo =
          if (TypeTree.of(using tpe).tpe =:= TypeTree.of[Boolean].tpe) then "Boolean | true | false"
          else formatType(using tpe)
        if isChinese then {
          s"""该属性要求类型: ${tpeInfo}, 实际值为: ${value}""".stripMargin
        } else {
          s"""This property requires a type: ${tpeInfo}, actual value is: ${value}""".stripMargin
        }
      }

    def raiseError(msg: String)(using quotes: Quotes, position: MacrosPosition): Nothing = {
      import quotes.reflect.*
      report.errorAndAbort(msg + s"\n${position}")
    }

    def logInfo(msg: String)(using quotes: Quotes, position: MacrosPosition): Unit = {
      import quotes.reflect.*
      report.info(msg + s"\n${position}")
    }
  }

  case class MacrosPosition(file: File, line: Line, name: FullName) {
    override def toString: String = s"${file.value}:${line.value}"
  }

  object MacrosPosition {

    given generate(using file: File, line: Line, name: FullName): MacrosPosition =
      MacrosPosition(file, line, name)
  }

  given nullToExpr[T <: String | scala.Null]: ToExpr[T] with {

    override def apply(x: T)(using quotes: Quotes): Expr[T] = {
      import quotes.reflect.*
      if x == null then Literal(NullConstant()).asExpr.asInstanceOf[Expr[T]]
      else Literal(StringConstant(x.asInstanceOf[String])).asExpr.asInstanceOf[Expr[T]]
    }
  }

}
