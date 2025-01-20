package scala.xml
package binders

import com.raquo.laminar.api.L.MountContext
import org.scalajs.dom

object HooksApi {
  type MountFunc        = dom.Element => Unit
  type MountFuncUnit    = () => Unit
  type MountFuncContext = MountContext[ReactiveElementBase] => Unit
  type MountFuncValid   = MountFunc | MountFuncUnit | MountFuncContext

  type UnMountFunc        = dom.Element => Unit
  type UnMountFuncUnit    = () => Unit
  type UnMountFuncContext = ReactiveElementBase => Unit
  type UnMountFuncValid   = UnMountFunc | UnMountFuncUnit | UnMountFuncContext

  trait ToMountFunc[T] {
    def apply(fun: T): MountFuncContext
  }

  object ToMountFunc {
    given func: ToMountFunc[MountFunc]       = fun => (e: MountContext[ReactiveElementBase]) => fun(e.thisNode.ref)
    given unit: ToMountFunc[MountFuncUnit]   = fun => (e: MountContext[ReactiveElementBase]) => fun()
    given ctx: ToMountFunc[MountFuncContext] = fun => fun
  }

  trait ToUnMountFunc[T] {
    def apply(fun: T): UnMountFuncContext
  }

  object ToUnMountFunc {
    given func: ToUnMountFunc[UnMountFunc]       = (fun) => (e: ReactiveElementBase) => fun(e.ref)
    given unit: ToUnMountFunc[UnMountFuncUnit]   = (fun) => (e: ReactiveElementBase) => fun()
    given ctx: ToUnMountFunc[UnMountFuncContext] = (fun) => fun
  }

  object Mount {

    inline def apply(inline fun: dom.Element => Unit): dom.Element => Unit = {
      fun
    }

    inline def context(
      inline fun: MountContext[ReactiveElementBase] => Unit): MountContext[ReactiveElementBase] => Unit = {
      fun
    }

    inline def unit(func: () => Unit): () => Unit = {
      func
    }
  }

  object UnMount {

    inline def apply(inline fun: dom.Element => Unit): dom.Element => Unit = {
      fun
    }

    inline def context(inline fun: ReactiveElementBase => Unit): ReactiveElementBase => Unit = {
      fun
    }

    inline def unit(func: () => Unit): () => Unit = {
      func
    }
  }
}
