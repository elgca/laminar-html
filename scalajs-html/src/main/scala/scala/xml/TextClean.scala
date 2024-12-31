package scala.xml

import scala.quoted.*

object TextClean {

  inline def trimOrDropTextNode(inline node: Text, underlying: NodeBuffer): NodeBuffer = {
    ${ trimOrDropTextNodeMacros('node, 'underlying) }
  }

  private def trimOrDropTextNodeMacros(
    nodeExpr: Expr[Text],
    underlying: Expr[NodeBuffer],
  )(using Quotes): Expr[NodeBuffer] = {
    import quoted.*
    import quotes.reflect.*
    val tree         = nodeExpr.asTerm
    val trimmedValue =
      tree match {
        case Inlined(
              _,
              _,
              Inlined(
                _,
                _,
                x @ Apply(
                  Select(
                    New(
                      tpt, // "scala.xml.Text"
                    ),
                    _),
                  List(Literal(StringConstant(value))),
                ),
              ),
            ) if tpt.tpe =:= TypeTree.of[Text].tpe =>
          // 检查是否是Text节点，并且有一个字符串字面量作为参数
          value.trim
        case _ =>
          report.error("Could not find Test")
          ""
      }
    if (trimmedValue.isEmpty) underlying // 空Text不添加
    else '{ $underlying.&+(${ Expr(trimmedValue) }) }
  }
}
