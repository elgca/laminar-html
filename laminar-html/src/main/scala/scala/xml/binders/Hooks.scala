package scala.xml
package binders

import com.raquo.laminar.api.L.MountContext
import org.scalajs.dom

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

object Hooks {
  type MountFunc        = dom.Element => Unit
  type MountFuncUnit    = () => Unit
  type MountFuncContext = MountContext[ReactiveElementBase] => Unit

  type UnMountFunc        = dom.Element => Unit
  type UnMountFuncUnit    = () => Unit
  type UnMountFuncContext = ReactiveElementBase => Unit

  def unapply(e: String): Option[String] = {
    Option(e)
      .map(_.toLowerCase)
      .filter(hooks.contains)
  }

  val hooks = Set(
    "onunmount",
    "onmount",
  )
}
