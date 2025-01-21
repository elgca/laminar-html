package scala.xml
package binders

import scala.quoted.*

object Events {

  def unapply(e: String)(using Quotes): Option[String] = {
    if e.startsWith("on") then eventKey.find(key => key.equalsIgnoreCase(e.drop(2)))
    else None
  }

  class EventsMacros(val eventKey: String)(using Quotes) {

    def addEventListener[T: Type](processor: Expr[T]): Expr[MetatDataBinder] = {
      import EventsApi.*
      val conversion: Option[Expr[ToJsListener[T]]] = Expr.summon[ToJsListener[T]]
      conversion match
        case Some(funConversion) =>
          '{
            EventsApi.addEventListener(${ Expr(eventKey) }, ${ funConversion }.apply(${ processor }))
          }
        case None                => MacorsMessage.unsupportEventType[T]
    }

    def addEventListenerFromSource[V: Type, CC <: Source[V]: Type](processor: Expr[CC]): Expr[MetatDataBinder] = {
      import EventsApi.*
      val conversion: Expr[ToJsListener[V]] =
        Expr.summon[ToJsListener[V]].getOrElse(MacorsMessage.unsupportEventType[V])
      '{
        EventsApi.addEventListener(${ Expr(eventKey) }, ${ processor }, ${ conversion })
      }
    }
  }

  object EventsMacros {

    def unapply(e: String)(using Quotes): Option[EventsMacros] = {
      Events.unapply(e).map { key =>
        EventsMacros(key)
      }
    }
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
