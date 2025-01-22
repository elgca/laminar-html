package scala.xml
package binders

import org.scalajs.dom

import scala.scalajs.js

object EventsApi {
  type JsListener = js.Function1[? <: dom.Event, Unit]

  trait ToJsListener[T] {
    def apply(fun: T): JsListener
  }

  object ToJsListener {
    given unit: ToJsListener[() => Unit] = fun => (e: dom.Event) => fun()

    given event[Event <: dom.Event]: ToJsListener[Event => Unit] = fun => fun

    given str: ToJsListener[String => Unit] = fun => { (ev: dom.Event) =>
      fun(DomApi.getValue(ev.target.asInstanceOf[dom.Element]).getOrElse(""))
    }

    given checked: ToJsListener[Boolean => Unit] = fun => { (ev: dom.Event) =>
      fun(DomApi.getChecked(ev.target.asInstanceOf[dom.Element]).getOrElse(false))
    }

    given file: ToJsListener[List[dom.File] => Unit] = fun => { (ev: dom.Event) =>
      fun(DomApi.getFiles(ev.target.asInstanceOf[dom.Element]).getOrElse(List.empty))
    }

    type ListenerFuncTypes = (() => Unit) | //
      ((? <: dom.Event) => Unit) | //
      (String => Unit) | //
      (Boolean => Unit) | //
      (List[dom.File] => Unit)
  }

  def addEventListener(
    name: String,
    listener: JsListener,
  ): MetatDataBinder = (ns, element) => {
    val subscribe = (ctx: MountContext[ReactiveElement.Base]) => {
      element.ref.addEventListener(name, listener)
      new Subscription(
        ctx.owner,
        cleanup = () => { element.ref.removeEventListener(name, listener) },
      )
    }

    val _ = ReactiveElement.bindSubscriptionUnsafe(element)(subscribe)
  }

  def addEventListener[T](
    name: String,
    listener: Source[T],
    conversion: ToJsListener[T],
  ): MetatDataBinder = (ns, element) => {
    ReactiveElement.bindSubscriptionUnsafe(element) { c =>
      var before: Option[JsListener] = None
      def clearBefore()              = before match {
        case Some(beforeListener) =>
          element.ref.removeEventListener(name, beforeListener)
          before = None
        case None                 =>
      }

      listener.toObservable.foreach { nextValue =>
        val listener = conversion.apply(nextValue)
        clearBefore()
        element.ref.addEventListener(name, listener)
        before = Some(listener)
      }(c.owner)

      new Subscription(c.owner, cleanup = () => clearBefore())
    }
  }
}
