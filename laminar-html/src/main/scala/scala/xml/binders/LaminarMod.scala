package scala.xml
package binders

import scala.quoted.*

object LaminarMod {
  trait ModEvidence[T]

  object ModEvidence {
    // Laminar Mod
    given modEvidence[Ref <: LAnyMod]: ModEvidence[Ref] = new ModEvidence[Ref] {}
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
