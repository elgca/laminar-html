package xhtml
package interpolator

import xhtml.interpolator.Tree.*

import scala.quoted.*
import scala.xml.NodeBuffer.AcceptableNode

object Expand:

  def apply(nodes: Seq[Node])(using XmlContext, Quotes, Reporter): Expr[scala.xml.Node | scala.xml.NodeBuffer] =
    if nodes.size == 1 then expandNode(nodes.head).asInstanceOf[Expr[scala.xml.Node]]
    else expandNodes(nodes)

  private def expandNode(node: Node)(using XmlContext, Quotes, Reporter): Expr[scala.xml.Node] =
    node match
      case elem: Elem               => expandElem(elem)
      case text: Text               => expandChildNode(Expr(text.text), text.pos)
      case comment: Comment         => expandChildNode('{ new scala.xml.Comment(${ Expr(comment.text) }) }, comment.pos)
      case placeholder: Placeholder => expandPlaceholder(placeholder)
      case _                        =>
        reporter.error(s"Unsupport Node:${node.getClass.getName}", node.pos)
  end expandNode

  private def expandNodes(nodes: Seq[Node])(using
    ctx: XmlContext,
    quotes: Quotes,
    reporter: Reporter,
  ): Expr[scala.xml.NodeBuffer] = {
    import quotes.reflect.*
    '{
      val buf = scala.xml.NodeBuffer()
      ${
//        println(s"---->|" + nodes.headOption.map {
//          case placeholder: Placeholder => {
//            val arg       = ctx.args(placeholder.id)
//            val scalaCode = Expr.betaReduce(arg)
//            val tpe       = scalaCode.asTerm.tpe.widen
////            TypeApply
//            val term      = Select.overloaded(
//              'buf.asTerm,
////              Symbol.requiredMethod("&+")
//              "&+",
//              tpe :: Nil,
//              scalaCode.asTerm :: Nil,
//            )
//
//            term.etaExpand(Symbol.spliceOwner).asExpr//.show// + "###" + '{ buf.&+ }.asTerm
//          }
//          case node                        => '{ buf.&+(${ expandNode(node) }) }
//        })
        Expr.block(
          nodes.map(node => '{ buf.&+(${ expandNode(node) }) }).toList,
//          nodes.map {
//            case placeholder: Placeholder => {
//              val arg       = ctx.args(placeholder.id)
//              val scalaCode = Expr.betaReduce(arg)
//              val tpe       = scalaCode.asTerm.tpe.widen
//              //            TypeApply
//              val term      = Select.overloaded(
//                'buf.asTerm,
//                //              Symbol.requiredMethod("&+")
//                "&+",
//                tpe :: Nil,
//                scalaCode.asTerm :: Nil,
//              )
//
//              term.etaExpand(Symbol.spliceOwner).asExpr // .show// + "###" + '{ buf.&+ }.asTerm
//            }
//            case node                     => '{ buf.&+(${ expandNode(node) }) }
//          }.toList,
          'buf,
        )
      }
    }
//    nodes.foldLeft('{ val _buf = scala.xml.NodeBuffer(); _buf }): (expr, node) =>
//      '{ ${ expr }.&+(${ expandNode(node) }); ${ expr } }
  }

  private def expandElem(elem: Elem)(using ctx: XmlContext, q: Quotes, reporter: Reporter): Expr[scala.xml.Elem] =
    val (namespaces, attributes) = elem.attributes.partition(_.isNamespace)
    val prefix                   = if elem.prefix.nonEmpty then Expr(elem.prefix) else '{ null }
    val label                    = Expr(elem.label)
    val attributes1              = expandAttributes(attributes)
    val scope                    = expandNamespaces(namespaces)
    val empty                    = Expr(elem.end.isEmpty)
    val child                    = expandNodes(elem.children)(using new XmlContext(ctx.args, scope), q)
    if elem.children.isEmpty then '{ new scala.xml.Elem($prefix, $label, $attributes1, $scope, $empty) }
    else '{ new scala.xml.Elem($prefix, $label, $attributes1, $scope, $empty, $child*) }
  end expandElem

  private def expandAttributes(attributes: Seq[Attribute])(using
    ctx: XmlContext,
    quotes: Quotes,
    reporter: Reporter,
  ): Expr[scala.xml.MetaData] =
    import quotes.reflect.*
    attributes.foldRight('{ _root_.scala.xml.Null }: Expr[scala.xml.MetaData]): (attribute, rest) =>
      val value: Expr[Any] = attribute.value match
        case Some(Text(str))          => Expr(str)
        case Some(Placeholder(value)) => Expr.betaReduce(ctx.args(value))
        case None                     => Expr(true)
        case Some(other)              =>
          reporter.error(s"attrs type mismatch; found   : ${other}", attribute.pos)

      val term = value.asTerm

      val AppliedType(sourceType, _) = TypeRepr.of[scala.xml.Source[?]]: @unchecked

      val pos = scala.xml.MacrosPosition.generate.copy(
        pos = Some(reporter.position(attribute.pos)),
      )
      term.tpe.widen match {
        case cc @ AppliedType(c, v :: Nil) if cc <:< AppliedType(sourceType, v :: Nil) => {
          val namespaceURI = scala.xml.TopScope.namespaceURI(attribute.prefix)

          val binderExpr = scala.xml.MacrosTool.dattributeRxMacro[Any, scala.xml.Source[Any]](
            namespaceURI = namespaceURI,
            prefix = Option(attribute.prefix).filterNot(_.isEmpty),
            key = attribute.key,
            sourceValue = value.asInstanceOf[Expr[? <: scala.xml.Source[Any]]],
          )(using
            v.asType.asInstanceOf[Type[Any]],
            cc.asInstanceOf[Type[scala.xml.Source[Any]]],
            pos,
          )
          '{ scala.xml.MetaData(${ binderExpr }, $rest) }
        }
        case other                                                                     => {
          val namespaceURI = scala.xml.TopScope.namespaceURI(attribute.prefix)
          val binderExpr   = scala.xml.MacrosTool.attributeMacro(
            namespaceURI = namespaceURI,
            prefix = Option(attribute.prefix).filterNot(_.isEmpty),
            attrKey = attribute.key,
            expr = value,
          )(using other.asType.asInstanceOf[Type[Any]], pos)
          '{ scala.xml.MetaData(${ binderExpr }, $rest) }
        }
      }

//      if term.tpe <:< TypeRepr.of[String] then
//        val value = term.asExprOf[String]
//        if attribute.prefix.isEmpty then '{ scala.xml.UnprefixedAttribute(${ Expr(attribute.key) }, $value, $rest) }
//        else '{ scala.xml.PrefixedAttribute(${ Expr(attribute.prefix) }, ${ Expr(attribute.key) }, $value, $rest) }
//      else if attribute.prefix.isEmpty then '{ scala.xml.UnprefixedAttribute(${ Expr(attribute.key) }, $value, $rest) }
//      else '{ scala.xml.PrefixedAttribute(${ Expr(attribute.prefix) }, ${ Expr(attribute.key) }, $value, $rest) }
  end expandAttributes

  private def expandNamespaces(namespaces: Seq[Attribute])(using XmlContext, Quotes): Expr[Scope] =
    namespaces.foldLeft(ctx.scope): (rest, namespace) =>
      val prefix = if namespace.prefix.nonEmpty then Expr(namespace.key) else '{ null }
      val uri    = (namespace.value.head: @unchecked) match
        case Text(text)      => Expr(text)
        case Placeholder(id) =>
          val call = ctx.args(id)
          Expr.betaReduce(call).asExprOf[String]
      '{ scala.xml.NamespaceBinding($prefix, $uri, $rest) }
  end expandNamespaces

  private def expandChildNode[T: Type](data: Expr[T], pos: Int)(using
    quotes: Quotes,
    reporter: Reporter): Expr[scala.xml.Node] = {
    import quotes.reflect.*
    Implicits.search(TypeRepr.of[AcceptableNode[T]]) match {
      case iss: ImplicitSearchSuccess =>
        '{ ${ iss.tree.asExpr.asInstanceOf[Expr[AcceptableNode[T]]] }.asNode(${ data }) }
      case isf: ImplicitSearchFailure =>
        reporter.error(isf.explanation, pos)
    }
  }

  private def expandPlaceholder(placeholder: Placeholder)(using
    ctx: XmlContext,
    quotes: Quotes,
    reporter: Reporter,
  ): Expr[scala.xml.Node] =
    import quotes.*
    import quotes.reflect.*
    val arg                       = ctx.args(placeholder.id)
    val scalaCode                 = Expr.betaReduce(arg)
    val tpe                       = scalaCode.asTerm.tpe.widen
    val AppliedType(acceptTpe, _) = TypeRepr.of[AcceptableNode[String]]: @unchecked

    val acceptableNodeType = AppliedType(acceptTpe, List(tpe))
//    println("=======>" + Type.show(using tpe))
    Implicits.search(acceptableNodeType) match {
      case iss: ImplicitSearchSuccess =>
        '{ ${ iss.tree.asExpr.asInstanceOf[Expr[AcceptableNode[Any]]] }.asNode(${ scalaCode }) }
      case isf: ImplicitSearchFailure =>
        reporter.error("-->" + isf.explanation, placeholder.pos)
    }

end Expand
