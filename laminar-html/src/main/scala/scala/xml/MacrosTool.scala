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
  inline def trimOrDropTextNode(inline node: Text, underlying: NodeBuffer): Unit = {
    ${ trimOrDropTextNodeMacros('node, 'underlying) }
  }

  private def trimOrDropTextNodeMacros(
    nodeExpr: Expr[Text],
    underlying: Expr[NodeBuffer],
  )(using quotes: Quotes): Expr[Unit] = {
    import quoted.*
    constTextValue(nodeExpr) match
      case Some(value) =>
        // 检查是否是Text节点，并且有一个字符串字面量作为参数
        val trimmedValue = value.trim
        if trimmedValue.isEmpty then '{ () } // 空Text不添加
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

  private def constValueIncludeSeq[T](expr: Expr[T])(using quotes: Quotes): Option[Seq[String]] = {
    import quotes.*
    def fromSeq[CC[x] <: Iterable[x]](expr: Expr[?])(using FromExpr[CC[String]]): Option[Iterable[String]] = {
      expr match
        case x: Expr[CC[String]] @unchecked => x.value
        case _                              => None
    }
    constAnyValue(expr)
      .map(x => x.toList)
      .orElse(fromSeq[Seq](expr))
      .orElse(fromSeq[List](expr))
      .orElse(fromSeq[Set](expr))
      .map(_.toSeq)
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

  def EmptyBinder(using Quotes) = {
    '{ MetaData.EmptyBinder }
  }

  def attributeMacro[T](
    namespaceURI: Option[String],
    prefix: Option[String],
    attrKey: String,
    expr: Expr[T],
  )(using
    tpe: Type[T],
    position: MacrosPosition,
  )(using quotes: Quotes): Expr[MetatDataBinder] = {
    import quotes.*
    import quotes.reflect.*

    val namespace  = namespaceFunction(namespaceURI, prefix)
    val constValue = constValueIncludeSeq(expr).map(_.mkString(" "))

    // 如果是常量,优先使用String匹配
    val typeIsString: Boolean = (TypeRepr.of[T] <:< TypeRepr.of[Text]) || (TypeRepr.of[T] <:< TypeRepr.of[String])
    val matchTpe              = if typeIsString then Type.of[String] else Type.of[T]

    val binderExpr: Expr[MetatDataBinder] = (prefix, attrKey, matchTpe) match {
      case AttrMacrosDef(name, macros) =>
        if !macros.supportConst then {
          constValue
            .filter(x => typeIsString)
            .fold(
              macros.withExpr(namespace, prefix, name, expr),
            )(value => {
              macros.withExpr(namespace, prefix, name, Expr(value))
            })
        } else {
          constValue
            .fold(
              macros.withExpr(namespace, prefix, name, expr),
            )(value => {
              macros.withConst(namespace, prefix, name, value)
            })
        }
      case _                           => ????
    }
    binderExpr
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

    val binderExpr: Expr[MetatDataBinder] = attributeMacro(
      namespaceURI,
      prefix,
      attrKey,
      valueExpr,
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

    val prefix       = Some(attrPrefix)
    // 如果我可以获取默认配置获取的namespaceURI,那么就不需要通过Elem输入了
    val namespaceURI = TopScope.namespaceURI(attrPrefix)

    val binderExpr: Expr[MetatDataBinder] = attributeMacro(
      namespaceURI,
      prefix,
      attrKey,
      valueExpr,
    )

    '{ MetaData(${ binderExpr }, ${ nextExpr }) }
  }

  // ------ Rx变量解析 ---------

  def dattributeRxMacro[V, CC <: Source[V]](
    namespaceURI: Option[String],
    prefix: Option[String],
    key: String,
    sourceValue: Expr[CC],
  )(using
    vTpe: Type[V],
    tpe: Type[CC],
    position: MacrosPosition,
  )(using quotes: Quotes): Expr[MetatDataBinder] = {
    val namespace = namespaceFunction(namespaceURI, prefix)
    (prefix, key, Type.of[V]) match {
      case AttrMacrosDef(name, macros) =>
        if !macros.supportSource then MacorsMessage.expectationType(using Type.of[CC], macros.expectType)
        macros.withExprFromSource(namespace, prefix, name, sourceValue)
      case _                           => ????
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
