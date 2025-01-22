package scala.xml
package binders

import scala.quoted.*

object Events {
  given infoType: MacorsMessage.AttrType = MacorsMessage.AttrType("Events")

  def unapply(inputKey: String)(using Quotes): Option[String] = {
    eventKey.find(key => {
      key.equalsIgnoreCase(inputKey) ||
      (inputKey.startsWith("on") && key.equalsIgnoreCase(inputKey.drop(2)))
    })
  }

  class EventsMacros(val eventKey: String)(using quotes: Quotes) {

    import EventsApi.*

    def checkType[T: Type]: Boolean = {
      conversion[T].isDefined
    }

    private def block[T: Type](body: => Expr[MetatDataBinder])(using MacrosPosition): Expr[MetatDataBinder] = {
      if conversion[T].isEmpty then MacorsMessage.unsupportEventType[T]
      MacorsMessage.showSupportedTypes[ToJsListener.ListenerFuncTypes]
      body
    }

    private def conversion[T: Type](using Quotes): Option[Expr[ToJsListener[T]]] = Expr.summon[ToJsListener[T]]

    def addEventListener[T: Type](processor: Expr[T])(using MacrosPosition): Expr[MetatDataBinder] = block[T] {
      '{
        EventsApi.addEventListener(${ Expr(eventKey) }, ${ conversion.get }.apply(${ processor }))
      }
    }

    def addEventListenerFromSource[V: Type, CC <: Source[V]: Type](
      processor: Expr[CC],
    )(using MacrosPosition): Expr[MetatDataBinder] =
      block[V] {
        '{
          EventsApi.addEventListener(${ Expr(eventKey) }, ${ processor }, ${ conversion.get })
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
