package scala.xml
package binders

import org.scalajs.dom

import scala.scalajs.js

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

class AddEventListener[T](key: String, fun: T, removeFunc: Option[T], listenerConversion: ToJsListener[T])
    extends ((NamespaceBinding, ReactiveElementBase) => Unit) {

  def apply(namespaceBinding: NamespaceBinding, element: ReactiveElementBase): Unit = {
    removeFunc.foreach(f => element.ref.removeEventListener(key, listenerConversion(f)))
    element.ref.addEventListener(key, listenerConversion(fun))
  }
}

object Events {
  def key(e: String): Option[String] = unapply(e)

  def unapply(e: String): Option[String] = {
    Some(e)
      .map(_.toLowerCase) // 事件类型全是小写,这里转换后使用,为了支持onClick这样设置事件属性
      .map(key => if key.startsWith("on") then key.drop(2) else key)
      .filter(eventKey.contains)
  }

  val eventKey = Set(
    "click",
    "dblclick",
    "mousedown",
    "mousemove",
    "mouseout",
    "mouseover",
    "mouseleave",
    "mouseenter",
    "mouseup",
    "wheel",
    "contextmenu",
    "drag",
    "dragend",
    "dragenter",
    "dragleave",
    "dragover",
    "dragstart",
    "drop",
    "pointerover",
    "pointerenter",
    "pointerdown",
    "pointermove",
    "pointerup",
    "pointercancel",
    "pointerout",
    "pointerleave",
    "gotpointercapture",
    "lostpointercapture",
    "touchstart",
    "touchmove",
    "touchcancel",
    "touchend",
    "change",
    "select",
    "beforeinput",
    "input",
    "blur",
    "focus",
    "submit",
    "reset",
    "invalid",
    "search",
    "keydown",
    "keyup",
    "keypress",
    "copy",
    "cut",
    "paste",
    "abort",
    "canplay",
    "canplaythrough",
    "cuechange",
    "durationchange",
    "emptied",
    "ended",
    "loadeddata",
    "loadedmetadata",
    "loadstart",
    "pause",
    "play",
    "playing",
    "progress",
    "ratechange",
    "seeked",
    "seeking",
    "stalled",
    "suspend",
    "timeupdate",
    "volumechange",
    "waiting",
    "animationend",
    "animationiteration",
    "animationstart",
    "transitionend",
    "load",
    "resize",
    "scroll",
    "selectstart",
    "show",
    "toggle",
    "error",
  )
}
