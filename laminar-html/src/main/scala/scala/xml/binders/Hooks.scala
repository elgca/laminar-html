package scala.xml
package binders

import scala.quoted.*
import scala.xml.MacorsMessage.????

object Hooks {

  def unapply(inputKey: String): Option[String] = {
    hooks.keys.find(key => {
      key.equalsIgnoreCase(inputKey) ||
      (inputKey.startsWith("on") && key.equalsIgnoreCase(inputKey.drop(2)))
    })
  }

  lazy val hooks = Map(
    "onunmount" -> HooksMacros.unMountHooks,
    "onmount"   -> HooksMacros.mountHooks,
  )

  trait HooksMacros {
    def withHooks[T: Type](funExpr: Expr[T])(using Quotes): Expr[MetatDataBinder]
  }

  object HooksMacros {
    import HooksApi.*

    def unapply(hookKey: String)(using Quotes): Option[HooksMacros] = {
      Hooks.unapply(hookKey).map(key => apply(key))
    }

    def apply(hookKey: String)(using Quotes): HooksMacros = {
      Hooks.hooks.getOrElse(hookKey, ????)
    }

    val mountHooks: HooksMacros = new HooksMacros:
      given infoType: MacorsMessage.AttrType = MacorsMessage.AttrType("mountHook")

      override def withHooks[T: Type](funExpr: Expr[T])(using Quotes): Expr[MetatDataBinder] = {
        val conversion: Option[Expr[ToMountFunc[T]]] = Expr.summon[ToMountFunc[T]]

        val callbackFunc = conversion
          .map(x => '{ ${ x }.apply(${ funExpr }) })
          .getOrElse(MacorsMessage.expectationType[T, MountFuncValid])
        MacorsMessage.showSupportedTypes[MountFuncValid]

        '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
          L.onMountCallback(${ callbackFunc }).apply(element)
        }
      }

    val unMountHooks: HooksMacros = new HooksMacros:
      given infoType: MacorsMessage.AttrType = MacorsMessage.AttrType("unMountHook")

      override def withHooks[T: Type](funExpr: Expr[T])(using Quotes): Expr[MetatDataBinder] = {
        val conversion: Option[Expr[ToUnMountFunc[T]]] = Expr.summon[ToUnMountFunc[T]]

        val callbackFunc = conversion
          .map(x => '{ ${ x }.apply(${ funExpr }) })
          .getOrElse(MacorsMessage.expectationType[T, UnMountFuncValid])
        MacorsMessage.showSupportedTypes[UnMountFuncValid]

        '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
          L.onUnmountCallback(${ callbackFunc }).apply(element)
        }
      }
  }

}
