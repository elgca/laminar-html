package scala.xml

import scala.quoted.{Expr, Quotes, ToExpr}

given nullToExpr[T <: String | scala.Null]: ToExpr[T] with {

  override def apply(x: T)(using quotes: Quotes): Expr[T] = {
    import quotes.reflect.*
    if x == null then Literal(NullConstant()).asExpr.asInstanceOf[Expr[T]]
    else Literal(StringConstant(x.asInstanceOf[String])).asExpr.asInstanceOf[Expr[T]]
  }
}
