package scala.xml
package binders

import scala.quoted.*
import scala.xml.MacorsMessage.????

object LaminarMod {
  trait ModEvidence[T]

  object ModEvidence {
    // Laminar Mod
    given modEvidence[Ref <: LAnyMod]: ModEvidence[Ref] = new ModEvidence[Ref] {}
  }

  class LaminarModMacros(using Quotes) extends AttrMacrosDef[LAnyMod] {
    override def supportSource: Boolean = false

    override def checkType[T: Type]: Boolean = {
      val ev: Option[Expr[ModEvidence[T]]] = Expr.summon[ModEvidence[T]]
      ev.isDefined
    }

    override protected def withConstImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      constStr: String)(using MacrosPosition): Expr[MetatDataBinder] = ????

    override protected def withExprImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      expr: Expr[T])(using MacrosPosition): Expr[MetatDataBinder] = {
      '{ LaminarModApi.bindMod(${ expr }.asInstanceOf[LModBase]) }
    }
  }

  object LaminarModMacros {

    def apply[T: Type](value: Expr[T])(using Quotes): Option[Expr[MetatDataBinder]] = {
      val ev: Option[Expr[ModEvidence[T]]] = Expr.summon[ModEvidence[T]]
      ev.map(x => {
        '{ LaminarModApi.bindMod(${ value }.asInstanceOf[LModBase]) }
      })
    }
  }
}
