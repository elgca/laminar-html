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
  }
}
