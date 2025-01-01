package scala.xml

import scala.quoted.*

object MacrosTool {

  inline def trimOrDropTextNode(inline node: Text, underlying: NodeBuffer): NodeBuffer = {
    ${ trimOrDropTextNodeMacros('node, 'underlying) }
  }

  private def trimOrDropTextNodeMacros(
    nodeExpr: Expr[Text],
    underlying: Expr[NodeBuffer],
  )(using Quotes): Expr[NodeBuffer] = {
    import quoted.*
    import quotes.reflect.*

    val tree = nodeExpr.asTerm
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
        val trimmedValue = value.trim
        if trimmedValue.isEmpty then underlying // 空Text不添加
        else '{ $underlying.&+(${ Expr(trimmedValue) }) }
      case _ =>
        report.info("This is not a const TextValue")
        '{ $underlying.&+($nodeExpr.data) }
    }
  }
}
