package scala.xml
package binders

import org.scalajs.dom

object HooksApi {
  type MountFunc[T <: dom.Element] = T => Unit
  type MountFuncUnit               = () => Unit
  type UnMountFuncEl               = ReactiveElementBase => Unit
  type MountFuncContext            = MountContext[ReactiveElementBase] => Unit
  type MountFuncValid              = MountFunc[? <: dom.Element] | MountFuncUnit | MountFuncContext | UnMountFuncEl

  type UnMountFunc[T <: dom.Element] = T => Unit
  type UnMountFuncUnit               = () => Unit
  type UnMountFuncContext            = ReactiveElementBase => Unit
  type UnMountFuncValid              = UnMountFunc[? <: dom.Element] | UnMountFuncUnit | UnMountFuncContext

  trait ToMountFunc[T] {
    def apply(fun: T): MountFuncContext
  }

  object ToMountFunc {

    given func[T <: dom.Element]: ToMountFunc[MountFunc[T]] = fun =>
      (e: MountContext[ReactiveElementBase]) => fun(e.thisNode.ref.asInstanceOf[T])
    given unit: ToMountFunc[MountFuncUnit]                  = fun => (e: MountContext[ReactiveElementBase]) => fun()
    given el: ToMountFunc[UnMountFuncEl]                    = fun => (e: MountContext[ReactiveElementBase]) => fun(e.thisNode)
    given ctx: ToMountFunc[MountFuncContext]                = fun => fun
  }

  trait ToUnMountFunc[T] {
    def apply(fun: T): UnMountFuncContext
  }

  object ToUnMountFunc {

    given func[T <: dom.Element]: ToUnMountFunc[UnMountFunc[T]] = (fun) =>
      (e: ReactiveElementBase) => fun(e.ref.asInstanceOf[T])
    given unit: ToUnMountFunc[UnMountFuncUnit]                  = (fun) => (e: ReactiveElementBase) => fun()
    given ctx: ToUnMountFunc[UnMountFuncContext]                = (fun) => fun
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
