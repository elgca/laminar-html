package scala.xml
package binders

import scala.quoted.*
import scala.xml.MacorsMessage.{????, AttrType}

object Hooks {

  def unapply[T](
    tuple: (Option[String], String, Type[T]),
  )(using quotes: Quotes): Option[(String, AttrMacrosDef[?], Seq[(String, String)])] = {
    val (pfx, inputKey, tpe) = tuple
    hooks.view
      .filterKeys(key => {
        key.equalsIgnoreCase(inputKey) ||
        (inputKey.startsWith("on") && key.equalsIgnoreCase(inputKey.drop(2)))
      })
      .mapValues(_.apply(quotes))
      .headOption
      .map(x => (x._1, x._2, Seq(x._2.supportedTypesMessage)))
  }

  def unapply(inputKey: String)(using Quotes): Option[String] = {
    hooks.keys.find(key => {
      key.equalsIgnoreCase(inputKey) ||
      (inputKey.startsWith("on") && key.equalsIgnoreCase(inputKey.drop(2)))
    })
  }

  val hooks = Map(
    "onunmount" -> ((q: Quotes) => HooksMacros.unMountHooks(using q)),
    "onmount"   -> ((q: Quotes) => HooksMacros.mountHooks(using q)),
  )

  trait HooksMacros[R](using Quotes, Type[R], AttrType) extends AttrMacrosDef[R] {
    def withHooks[T: Type](funExpr: Expr[T]): Expr[MetatDataBinder]

    override def supportSource: Boolean = false

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

    override def withExprFromSourceImpl[V: Type, CC <: Source[V]: Type](
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
      Hooks.hooks.getOrElse(hookKey, ????).apply(quotes)
    }

    def mountHooks(using Quotes): HooksMacros[MountFuncValid] = {
      given attrType: AttrType = AttrType("Hooks:<mount>")
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

    def unMountHooks(using Quotes): HooksMacros[UnMountFuncValid] = {
      given attrType: AttrType = AttrType("Hooks:<unmount>")
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
