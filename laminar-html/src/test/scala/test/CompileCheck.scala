package test

import scala.quoted.*

object CompileCheck {

  def inspectCode[T](x: Expr[T])(using Quotes): Expr[T] = {
    import quotes.*, quotes.reflect.*
    //    println(x.asTerm)
    println(x.show)
    x
  }

  inline def apply[T](inline x: T): T = ${ inspectCode('x) }

}
