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
    element.ref.addEventListener(name, listener)
  }

  def addEventListener[T](
    name: String,
    listener: Source[T],
    conversion: ToJsListener[T],
  ): MetatDataBinder = (ns, element) => {
    var before: Option[JsListener] = None
    ReactiveElement.bindFn(element, listener.toObservable) { nextValue =>
      val listener = conversion.apply(nextValue)
      before.foreach(beforeListener => element.ref.removeEventListener(name, beforeListener))
      element.ref.addEventListener(name, listener)
      before = Some(listener)
    }
  }
}
