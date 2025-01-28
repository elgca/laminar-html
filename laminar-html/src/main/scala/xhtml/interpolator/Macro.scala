package xhtml
package interpolator

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.quoted.*

object Macro:

  /** ??? */
  def impl(strCtxExpr: Expr[StringContext], argsExpr: Expr[Seq[Any]], scope: Expr[Scope])(using
    quotes: Quotes): Expr[scala.xml.Node | scala.xml.NodeBuffer] =
    import quotes.reflect.*
    (strCtxExpr, argsExpr) match
      case ('{ StringContext(${ Varargs(parts) }*) }, Varargs(args)) =>
        val (xmlStr, offsets) = encode(parts)

        given XmlContext = new XmlContext(args, scope)
        given Reporter   = new Reporter {

          def error(msg: String, idx: Int): Nothing = {
            val (part, offset) = Reporter.from(idx, offsets, parts)
            val pos            = part.asTerm.pos
            val (srcF, start)  = (pos.sourceFile, pos.start)
            report.errorAndAbort(msg, Position(srcF, start + offset, start + offset + 1))
          }

          def error(msg: String, expr: Expr[Any]): Nothing =
            report.errorAndAbort(msg, expr)

          override def position(idx: Int): (Int, Int) = {
            val (part, offset) = Reporter.from(idx, offsets, parts)
            val pos            = part.asTerm.pos
            val (srcF, start)  = (pos.sourceFile, pos.start)
            (start + offset, start + offset + 1)
          }
        }

        implCore(xmlStr)
  end impl

//  def implErrors(strCtxExpr: Expr[StringContext], argsExpr: Expr[Seq[Scope ?=> Any]], scope: Expr[Scope])
//                (using quotes:Quotes): Expr[List[(Int, String)]] =
//    import quotes.reflect.*
//    (strCtxExpr, argsExpr) match
//      case ('{ StringContext(${Varargs(parts)}*) }, Varargs(args)) =>
//        val errors = List.newBuilder[Expr[(Int, String)]]
//        val (xmlStr, offsets) = encode(parts)
//        given XmlContext = new XmlContext(args, scope)
//        given Reporter = new Reporter {
//
//          def error(msg: String, idx: Int): Unit = {
//            val (part, offset) = Reporter.from(idx, offsets, parts)
//            val start = part.asTerm.pos.start - parts(0).asTerm.pos.start
//            errors += Expr((start + offset, msg))
//          }
//
//          def error(msg: String, expr: Expr[Any]): Unit = {
//            val pos = expr.asTerm.pos
//            errors += Expr((pos.start, msg))
//          }
//        }
//        val _ = implCore(xmlStr)
//        Expr.ofList(errors.result())
//  end implErrors

  private def implCore(
    xmlStr: String)(using XmlContext, Reporter, Quotes): Expr[scala.xml.Node | scala.xml.NodeBuffer] =

    import Expand.apply as expand
    import Parse.apply as parse
//    import TypeCheck.apply as typecheck
    import Validate.apply as validate

    val interpolate =
      parse andThen
//      transform andThen
        validate andThen
//        typecheck andThen
        expand

    interpolate(xmlStr)
  end implCore

  private def encode(parts: Seq[Expr[String]])(using Quotes): (String, Array[Int]) =
    val sb = new StringBuilder()
    val bf = ArrayBuffer.empty[Int]

    def appendPart(part: Expr[String]) =
      bf += sb.length
      sb ++= part.valueOrAbort
      bf += sb.length

    def appendHole(index: Int) = sb ++= Hole.encode(index)

    for (part, index) <- parts.init.zipWithIndex do
      val _ = appendPart(part)
      appendHole(index)
    val _ = appendPart(parts.last)

    (sb.toString, bf.toArray)
  end encode

end Macro
