package scala.xml
package binders

import scala.quoted.*
import scala.xml.MacorsMessage.{????, AttrType}

object Hooks {

  def unapply(inputKey: String)(using Quotes): Option[String] = {
    hooks.keys.find(key => {
      key.equalsIgnoreCase(inputKey) ||
      (inputKey.startsWith("on") && key.equalsIgnoreCase(inputKey.drop(2)))
    })
  }

  def hooks(using Quotes) = Map(
    "onunmount" -> HooksMacros.unMountHooks,
    "onmount"   -> HooksMacros.mountHooks,
  )

  trait HooksMacros[R](using Quotes, Type[R], AttrType) extends AttrMacrosDef[R] {
    def withHooks[T: Type](funExpr: Expr[T]): Expr[MetatDataBinder]

    override protected def withConstImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      constStr: String)(using MacrosPosition): Expr[MetatDataBinder] = ????

    override protected def withExprImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      expr: Expr[T])(using MacrosPosition): Expr[MetatDataBinder] = withHooks(expr)

    override def withExprFromSource[V: Type, CC <: Source[V]: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      sourceValue: Expr[CC])(using MacrosPosition): Expr[MetatDataBinder] = ????
  }

  object HooksMacros {
    import HooksApi.*

    def unapply(hookKey: String)(using Quotes): Option[HooksMacros[?]] = {
      Hooks.unapply(hookKey).map(key => apply(key))
    }

    def apply(hookKey: String)(using Quotes): HooksMacros[?] = {
      Hooks.hooks.getOrElse(hookKey, ????)
    }

    def mountHooks(using Quotes): HooksMacros[MountFuncValid] = {
      given attrType: AttrType = AttrType("mount")
      new HooksMacros[MountFuncValid] {

        override def checkType[T: Type]: Boolean = conversion.nonEmpty

        def conversion[T: Type]: Option[Expr[ToMountFunc[T]]] = Expr.summon[ToMountFunc[T]]

        override def withHooks[T: Type](funExpr: Expr[T]): Expr[MetatDataBinder] = {
          val callbackFunc = conversion.map(x => '{ ${ x }.apply(${ funExpr }) }).get
          '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
            L.onMountCallback(${ callbackFunc }).apply(element)
          }
        }
      }
    }

    def unMountHooks(using Quotes, AttrType): HooksMacros[UnMountFuncValid] = {
      given attrType: AttrType = AttrType("unmount")
      new HooksMacros[UnMountFuncValid] {
        override def checkType[T: Type]: Boolean = conversion.nonEmpty

        def conversion[T: Type]: Option[Expr[ToUnMountFunc[T]]] = Expr.summon[ToUnMountFunc[T]]

        override def withHooks[T: Type](funExpr: Expr[T]): Expr[MetatDataBinder] = {
          val callbackFunc = conversion.map(x => '{ ${ x }.apply(${ funExpr }) }).get
          '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
            L.onUnmountCallback(${ callbackFunc }).apply(element)
          }
        }
      }
    }
  }

}
