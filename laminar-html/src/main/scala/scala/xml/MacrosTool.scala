package scala.xml

import scala.annotation.tailrec
import scala.quoted.*
import scala.xml.MacorsMessage.????
import scala.xml.binders.*

object MacrosTool {

  /** NodeBuffer添加Text节点时候执行, 由于xml字面量会带着一些空格插入,但是这个是不需要的
    * {{{Text("   "), Text("   \nxxx\n   ")}}}
    * 所以执行:{{{Text(value) => val a = value.trim;
    * if(a.noEmpty) add(a) }}}
    *
    * 如果需要换行文本,使用嵌入对象方式例如: {{{ <code>{""" a x ... xxx
    * """"}</code> }}}
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
          MacorsMessage.unsupportConstProp(tpe.toString)(using tpe.asType)
        case Literal(value: Constant)      =>
          val constValue = value.value
          Some(Option(constValue).map(_.toString))
        case Ident("Nil")                  => NullConst // 处理一种很奇怪的场景, 当为 <div propkey="" />时候,常量为Nil
        case Typed(e, _)                   => rec(e)
        case Block(Nil, e)                 => rec(e)
        case Inlined(_, Nil, e)            => rec(e)
        case _                             => None
    }

    def fromOpt[ConstType: FromExpr: Type](e: Expr[T]): Option[Option[String]] = {
      expr match {
        case '{ new Some[ConstType](${ Expr(value) }) } => Some(Some(value.toString))
        case '{ Some[ConstType](${ Expr(value) }) }     => Some(Some(value.toString))
        case '{ Option[ConstType](${ Expr(value) }) }   => Some(Some(value.toString))
        case '{ None }                                  => Some(None)
        case _                                          => None
      }
    }

    rec(expr.asTerm)
      .orElse(fromOpt[String](expr))
      .orElse(fromOpt[Double](expr))
      .orElse(fromOpt[Boolean](expr))
      .orElse(fromOpt[Float](expr))
      .orElse(fromOpt[Char](expr))
      .orElse(fromOpt[Byte](expr))
      .orElse(fromOpt[Short](expr))
      .orElse(fromOpt[Int](expr))
      .orElse(fromOpt[Long](expr))
  }

  private def typeEquals[Base: Type, Target: Type](using quotes: Quotes): Boolean = {
    import quotes.reflect.*
    TypeRepr.of[Base] <:< TypeRepr.of[Target]
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
  )(using quotes: Quotes): Expr[MetatDataBinder] = {
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
      case None                => MacorsMessage.unsupportEventType[T]
  }

  def compositeItem[T: Type](valueExpr: Expr[T])(using quotes: Quotes): Expr[List[String]] = {
    import AttrsApi.*
    val constStr = constAnyValue(valueExpr)
    if constStr.isDefined then {
      val items = constStr.flatten.getOrElse("").split(" ").filter(_.nonEmpty).toList
      Expr(items)
    } else {
      Expr.summon[CompositeNormalize[T]] match
        case Some(value) => '{ ${ value }.apply(${ valueExpr }) }
        case None        => MacorsMessage.expectationType[T, CompositeNormalize.CompositeValidTypes]
    }
  }

  def namespaceFunction(
    namespaceURI: Option[String],
    prefix: Option[String],
  )(using quotes: Quotes): Expr[NamespaceBinding => Option[String]] = {
    import AttrsApi.*
    prefix
      .map(px => {
        if namespaceURI.isDefined then '{ namespaceWithURI(${ Expr(namespaceURI) }) }
        else '{ namespaceWithPrefix(${ Expr(px) }) }
      })
      .getOrElse('{ noNamespace })
  }

  def bindCompositeAttribute(
    namespaceURI: Option[String],
    prefix: Option[String],
    attrKey: String,
    itemsToAdd: Expr[List[String]],
    itemsToRemove: Expr[List[String]],
  )(using quotes: Quotes): Expr[MetatDataBinder] = {
    val name   = prefix.map(_ + ":" + attrKey).getOrElse(attrKey)
    val nsFunc = namespaceFunction(namespaceURI, prefix)
    '{ AttrsApi.setCompositeAttributeBinder(${ nsFunc }, ${ Expr(name) }, ${ itemsToAdd }, ${ itemsToRemove }) }
  }

  def bindAttribute[T: Type](
    namespaceURI: Option[String],
    prefix: Option[String],
    attrKey: String,
    valueExpr: Expr[T],
  )(using quotes: Quotes): Expr[MetatDataBinder] = {
    import Attrs.AttrMacros
    val nsFunc = namespaceFunction(namespaceURI, prefix)
    val name   = prefix.map(_ + ":" + attrKey).getOrElse(attrKey)
    val macros = AttrMacros.withKey(attrKey)

    val constStr: Option[String | Null] = constAnyValue(valueExpr).map(_.orNull)
    constStr match {
      case Some(value) => macros.withConst(nsFunc, name, value)
      case None        => macros.withExpr(nsFunc, name, valueExpr)
    }
  }

  def attrProvider(
    key: String,
  )(using quotes: Quotes): Option[Expr[Attrs.AttrProvider[?]]] = {
    import quotes.*
    import quotes.reflect.*

    // 这里尝试获取隐式 AttributeBinder["hello", T] , hello是key的常量类型
    val keyConstType          = ConstantType(StringConstant(key))
    val binderSymbol          = Symbol.classSymbol(classOf[Attrs.AttrProvider[?]].getName)
    val clsTpe                = TypeTree.ref(binderSymbol).tpe
    val providerType: Type[?] = AppliedType(clsTpe, List(keyConstType)).asType
    Implicits.search(TypeRepr.of(using providerType)) match {
      case iss: ImplicitSearchSuccess => Some(iss.tree.asExpr.asInstanceOf[Expr[Attrs.AttrProvider[?]]])
      case isf: ImplicitSearchFailure => None
    }
  }

  def bindUnknownAttribute[T: Type](
    namespaceURI: Option[String],
    prefix: Option[String],
    attrKey: String,
    valueExpr: Expr[T],
  )(using quotes: Quotes): Expr[MetatDataBinder] = {
    import Attrs.AttrMacros
    val nsFunc = namespaceFunction(namespaceURI, prefix)
    val name   = prefix.map(_ + ":" + attrKey).getOrElse(attrKey)
    val macros = AttrMacros.StringAttr

    MacorsMessage.notDefineAttrKey(name, valueExpr)

    val constStr: Option[String | Null] = constAnyValue(valueExpr).map(_.orNull)
    constStr match {
      case Some(value) => macros.withConst(nsFunc, name, value)
      case None        => macros.withExpr(nsFunc, name, valueExpr)
    }
  }

  def EmptyBinder(using Quotes) = {
    '{ MetaData.EmptyBinder }
  }

  def bindHtmlProp[T: Type](
    propKey: String,
    valueExpr: Expr[T],
  )(using quotes: Quotes): Expr[MetatDataBinder] = {
    val macors = Props.PropMacros.withKey(propKey)
    constAnyValue(valueExpr) match {
      case Some(value) => value.map(str => macors.withConst(str)).getOrElse(EmptyBinder)
      case None        => macors.withExpr(valueExpr)
    }
  }

//  def implicitBaseBinder[T: Type](
//    namespaceURI: Option[String],
//    prefix: Option[String],
//    key: String,
//    valueExpr: Expr[T],
//  )(using quotes: Quotes): Option[Expr[MetatDataBinder]] = {
//    import quotes.*
//    import quotes.reflect.*
//
//    // 这里尝试获取隐式 AttributeBinder["hello", T] , hello是key的常量类型
//    val keyConstType       = ConstantType(StringConstant(key))
//    val binderSymbol       = Symbol.classSymbol(classOf[BaseBinder[?, ?]].getName)
//    val clsTpe             = TypeTree.ref(binderSymbol).tpe
//    val binderTpe: Type[?] = AppliedType(clsTpe, List(keyConstType, TypeRepr.of[T])).asType
//
//    Implicits.search(TypeRepr.of(using binderTpe)) match {
//      case iss: ImplicitSearchSuccess => {
//        val binderExpr: Expr[BaseBinder[String, T]] =
//          iss.tree.asExpr.asInstanceOf[Expr[BaseBinder[String, T]]]
//
//        '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
//          {
//            ${ binderExpr }.bindAttr(
//              element,
//              None,
//              None,
//              ${ Expr(key) },
//              ${ valueExpr },
//            )
//          }
//        }
//      }
//      case isf: ImplicitSearchFailure =>
//    }
//  }

  def dattributeMacro[T: Type](
    namespaceURI: Option[String],
    prefix: Option[String],
    key: String,
    valueExpr: Expr[T],
  )(using quotes: Quotes): Expr[MetatDataBinder] = {
    // 在xhtml中嵌入lamianr的Modifier
    Expr.summon[BaseBinder[String, T]] match {
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
      case Hooks.HooksMacros(hooks) if prefix.isEmpty => hooks.withHooks(valueExpr)
      case Events(eventKey) if prefix.isEmpty         => bindEvent(eventKey, valueExpr, Expr(None))

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
        bindUnknownAttribute(
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

    val binderExpr: Expr[MetatDataBinder] = dattributeMacro(
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
    val namespaceURI                      = TopScope.namespaceURI(attrPrefix)
    val binderExpr: Expr[MetatDataBinder] = dattributeMacro(
      namespaceURI = namespaceURI,
      prefix = Some(attrPrefix),
      key = attrKey,
      valueExpr = valueExpr,
    )

    '{ MetaData(${ binderExpr }, ${ nextExpr }) }
  }

  // ------ Rx变量解析 ---------

  def dattributeRxMacro[V: Type, CC <: Source[V]: Type](
    namespaceURI: Option[String],
    prefix: Option[String],
    key: String,
    sourceValue: Expr[CC],
  )(using quotes: Quotes): Expr[MetatDataBinder] = {
    key match {
      case "value" if prefix.isEmpty          => {
        sourceValue match {
          case sourceStr: Expr[Source[String]] @unchecked if typeEquals[V, String] =>
            '{ PropsApi.valuePropUpdater(${ sourceStr }) }

          case _ => MacorsMessage.expectationType[CC, Source[String]]
        }
      }
      case Events(eventKey) if prefix.isEmpty => {
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

      case Attrs(attrKey) if Attrs.isComposite(attrKey) => {
        '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
          var before: List[String] = Nil
          ReactiveElement.bindFn(element, ${ sourceValue }.toObservable) { nextValue =>
            val addItems = ${ compositeItem(valueExpr = 'nextValue) }
            ${
              val updaterExpr: Expr[MetatDataBinder] = bindCompositeAttribute(
                namespaceURI = namespaceURI,
                prefix = prefix,
                attrKey = attrKey,
                itemsToAdd = 'addItems,
                itemsToRemove = 'before,
              )
              updaterExpr
            }.apply(ns, element)
            before = addItems
          }
        }
      }

      case otherKeys => {
        '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
          ReactiveElement.bindFn(element, ${ sourceValue }.toObservable) { nextValue =>
            ${
              val updaterExpr: Expr[MetatDataBinder] = dattributeMacro(
                namespaceURI = namespaceURI,
                prefix = prefix,
                key = otherKeys,
                valueExpr = 'nextValue,
              )
              updaterExpr
            }.apply(ns, element)
          }
        }
      }
    }
  }

  def unprefixedattributeRxMacro[V: Type, CC <: Source[V]: Type](
    attr: Expr[UnprefixedAttribute[CC]],
  )(using quotes: Quotes): Expr[MetaData] = {
    import quotes.*
    import quotes.reflect.*

    // 这里应该是安全的
    val '{ new UnprefixedAttribute(${ keyExpr }, ${ valueExpr: Expr[CC] }, ${ nextExpr }) } = attr: @unchecked

    val Literal(StringConstant(key: String)) = keyExpr.asTerm: @unchecked
    val prefix: Option[String]               = None
    val namespaceURI: Option[String]         = None

    val binderExpr: Expr[MetatDataBinder] = dattributeRxMacro[V, CC](
      namespaceURI = namespaceURI,
      prefix = prefix,
      key = key,
      sourceValue = valueExpr,
    )

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
    val namespaceURI = TopScope.namespaceURI(attrPrefix)

    val binderExpr: Expr[MetatDataBinder] = dattributeRxMacro[V, CC](
      namespaceURI = namespaceURI,
      prefix = Option(attrPrefix),
      key = attrKey,
      sourceValue = valueExpr,
    )

    '{ MetaData(${ binderExpr }, ${ nextExpr }) }
  }
}
