package scala.xml
package binders

import org.scalajs.dom
import org.scalajs.dom.EventListenerOptions

import scala.scalajs.js

object EventsApi {
  type JsListener = js.Function1[? <: dom.Event, Unit]

  trait ListenerConversion[T] {
    def apply(fun: T): JsListener
  }

  object ListenerConversion {
    given dynamic: ListenerConversion[js.Dynamic => Unit] = fun => (e: dom.Event) => fun(e.asInstanceOf[js.Dynamic])

    given unit: ListenerConversion[() => Unit] = fun => (e: dom.Event) => fun()

    given event[Event <: dom.Event]: ListenerConversion[Event => Unit] = fun => fun

    given str: ListenerConversion[String => Unit] = fun => { (ev: dom.Event) =>
      fun(DomApi.getValue(ev.target.asInstanceOf[dom.Element]).getOrElse(""))
    }

    given checked: ListenerConversion[Boolean => Unit] = fun => { (ev: dom.Event) =>
      fun(DomApi.getChecked(ev.target.asInstanceOf[dom.Element]).getOrElse(false))
    }

    given file: ListenerConversion[List[dom.File] => Unit] = fun => { (ev: dom.Event) =>
      fun(DomApi.getFiles(ev.target.asInstanceOf[dom.Element]).getOrElse(List.empty))
    }

  }

  class EventListener(
    val options: dom.EventListenerOptions,
    val callback: JsListener,
  )

  trait ToJsListener[T] {
    def apply(fun: T): EventListener
  }

  object ToJsListener {
    //      Listener[Event] | //
    //      (dom.EventListenerOptions, Listener[Event]) | //
    //      (Listener[Event] | dom.EventListenerOptions)

    // 用于校验的类型信息
    type ListenerFuncTypes[Event <: dom.Event] = (() => Unit) | //
      ((? >: Event) => Unit) | //
      (String => Unit) | //
      (Boolean => Unit) | //
      (List[dom.File] => Unit) | //
      (js.Dynamic => Unit) | //
      (dom.EventListenerOptions, (() => Unit)) | //
      (dom.EventListenerOptions, ((? >: Event) => Unit)) | //
      (dom.EventListenerOptions, (String => Unit)) | //
      (dom.EventListenerOptions, (Boolean => Unit)) | //
      (dom.EventListenerOptions, (List[dom.File] => Unit)) | //
      (dom.EventListenerOptions, (js.Dynamic => Unit)) | //
      ((() => Unit), dom.EventListenerOptions) | //
      (((? >: Event) => Unit), dom.EventListenerOptions) | //
      ((String => Unit), dom.EventListenerOptions) | //
      ((Boolean => Unit), dom.EventListenerOptions) | //
      ((List[dom.File] => Unit), dom.EventListenerOptions) | //
      ((js.Dynamic => Unit), dom.EventListenerOptions)

    given noneOptions[T: ListenerConversion]: ToJsListener[T] = (fun: T) =>
      EventListener(
        new EventListenerOptions { capture = false; passive = false },
        summon[ListenerConversion[T]].apply(fun))

    given withOptions[T: ListenerConversion]: ToJsListener[(dom.EventListenerOptions, T)] =
      (tuple: (dom.EventListenerOptions, T)) =>
        EventListener(
          tuple._1,
          summon[ListenerConversion[T]].apply(tuple._2),
        )

    given withOptions2[T: ListenerConversion]: ToJsListener[(T, dom.EventListenerOptions)] =
      (tuple: (T, dom.EventListenerOptions)) =>
        EventListener(
          tuple._2,
          summon[ListenerConversion[T]].apply(tuple._1),
        )
  }

  def addEventListener(
    name: String,
    listener: EventListener,
  ): MetatDataBinder = (ns, element) => {
    val subscribe = (ctx: MountContext[ReactiveElement.Base]) => {
      element.ref.addEventListener(name, listener.callback, listener.options)
      new Subscription(
        ctx.owner,
        cleanup = () => { element.ref.removeEventListener(name, listener.callback, listener.options); },
      )
    }

    val _ = ReactiveElement.bindSubscriptionUnsafe(element)(subscribe)
  }

  def addEventListener[T](
    name: String,
    listener: Source[T],
    conversion: ToJsListener[T],
  ): MetatDataBinder = (ns, element) => {
    val _ = ReactiveElement.bindSubscriptionUnsafe(element) { c =>
      var before: Option[EventListener] = None
      def clearBefore()                 = before match {
        case Some(beforeListener) =>
          element.ref.removeEventListener(name, beforeListener.callback, beforeListener.options)
          before = None
        case None                 =>
      }

      listener.toObservable.foreach { nextValue =>
        val listener = conversion.apply(nextValue)
        clearBefore()
        element.ref.addEventListener(name, listener.callback, listener.options)
        before = Some(listener)
      }(c.owner)

      new Subscription(c.owner, cleanup = () => clearBefore())
    }
  }
}
