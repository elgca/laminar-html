package scala.xml
package binders

import org.scalajs.dom

import scala.annotation.nowarn
import scala.collection.mutable.ListBuffer
import scala.quoted.*
import scala.xml.MacorsMessage.{????, AttrType}

object Events {
  given infoType: MacorsMessage.AttrType = MacorsMessage.AttrType("Events")

  def unapply[T](
    tuple: (Option[String], String, Type[T]),
  )(using quotes: Quotes): Option[(String, AttrMacrosDef[?], Seq[(String, String)])] = {
    val (_: Option[String], attrKey: String, tpe: Type[T]) = tuple

    val events = eventsDefine.value.iterator.flatMap { case (name, attr, factory) =>
      attr(attrKey)
        .map(key => key -> factory(quotes))
    }.toSeq

    val typeHints = events.map(_._2.supportedTypesMessage).distinct
    events
      .headOrMatch(_._2.checkType(using tpe))
      .map(x => (x._1, x._2, typeHints))
  }

  def unknownEvents(eventKey: String)(using
    quotes: Quotes,
  ): AttrMacrosDef[?] = {
    EventsMacros[dom.Event](eventKey)(using quotes, AttrType(s"Unknown Events :<${eventKey}>[dom.Event]"))
  }

  import EventsApi.*
  import EventsApi.ToJsListener.ListenerFuncTypes

  class EventsMacros[Ev <: dom.Event](
    val eventKey: String,
  )(using
    quotes: Quotes,
    attrType: AttrType,
  )(using Type[Ev])
      extends AttrMacrosDef[ListenerFuncTypes[Ev]] {

    override def checkType[T: Type]: Boolean = {
      import quotes.reflect.*

      (!StrictEventsFunction || TypeRepr.of[T] <:< TypeRepr.of[ListenerFuncTypes[Ev]]) &&
      conversion[T].isDefined
    }

    private def conversion[T: Type](using Quotes): Option[Expr[ToJsListener[T]]] = Expr.summon[ToJsListener[T]]

    override protected def withConstImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      constStr: String)(using MacrosPosition): Expr[MetatDataBinder] = ????

    override protected def withExprImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      processor: Expr[T])(using MacrosPosition): Expr[MetatDataBinder] = {
      '{
        EventsApi.addEventListener(${ Expr(eventKey) }, ${ conversion.get }.apply(${ processor }))
      }
    }

    override def withExprFromSourceImpl[V: Type, CC <: Source[V]: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      processor: Expr[CC])(using MacrosPosition): Expr[MetatDataBinder] = {
      '{
        EventsApi.addEventListener(${ Expr(eventKey) }, ${ processor }, ${ conversion[V].get })
      }
    }
  }

  val eventsDefine = new EventsDefine()
    with GlobalEventProps
    with DocumentEventProps
    with WindowEventProps
    with EventProps

  class EventsDefine(
    val value: ListBuffer[(String, String => Option[String], AttrMacrosDefFactory)] = ListBuffer.empty,
  ) {

    def attrType(name: String, evType: String)(using pos: MacrosPosition) = {
      val posName =
        pos.showNane.value // pos.name.value.split('.').dropRight(1).lastOption.getOrElse(pos.showNane.value)
      AttrType(s"${posName}:<${name}>[${evType}]")
    }

    def eventName(key: String) = { (inputKey: String) =>
      if key.equalsIgnoreCase(inputKey) ||
        (inputKey.startsWith("on") && key.equalsIgnoreCase(inputKey.drop(2))) then {
        Some(key)
      } else {
        None
      }
    }

    @nowarn
    inline given tpeProvider[Ev]: (Quotes => Type[Ev]) = (q: Quotes) => Type.of[Ev](using q)

    inline def eventProp[Ev <: dom.Event](name: String)(using tpe: Quotes => Type[Ev])(using pos: MacrosPosition) = {
      val evType = MacorsMessage.getTypeString[Ev]
      value.addOne {
        (
          name,
          eventName(name),
          (q: Quotes) => {
            given ev: Type[Ev] = tpe(q)
            new EventsMacros[Ev](name)(using q, attrType(name, evType))
          })
      }
    }
  }

  /** [[com.raquo.laminar.defs.eventProps.DocumentEventProps]]
    */
  trait DocumentEventProps { self: EventsDefine =>
    eventProp[dom.Event]("DOMContentLoaded")
    eventProp[dom.Event]("fullscreenchange")
    eventProp[dom.Event]("fullscreenerror")
    eventProp[dom.Event]("selectionchange")
    eventProp[dom.Event]("visibilitychange")
  }

  /** [[com.raquo.laminar.defs.eventProps.EventProps]] */
  trait EventProps { self: EventsDefine =>
    eventProp[dom.MouseEvent]("click")
    eventProp[dom.MouseEvent]("dblclick")
    eventProp[dom.MouseEvent]("mousedown")
    eventProp[dom.MouseEvent]("mousemove")
    eventProp[dom.MouseEvent]("mouseout")
    eventProp[dom.MouseEvent]("mouseover")
    eventProp[dom.MouseEvent]("mouseleave")
    eventProp[dom.MouseEvent]("mouseenter")
    eventProp[dom.MouseEvent]("mouseup")
    eventProp[dom.WheelEvent]("wheel")
    eventProp[dom.MouseEvent]("contextmenu")
    eventProp[dom.DragEvent]("drag")
    eventProp[dom.DragEvent]("dragend")
    eventProp[dom.DragEvent]("dragenter")
    eventProp[dom.DragEvent]("dragleave")
    eventProp[dom.DragEvent]("dragover")
    eventProp[dom.DragEvent]("dragstart")
    eventProp[dom.DragEvent]("drop")
    eventProp[dom.PointerEvent]("pointerover")
    eventProp[dom.PointerEvent]("pointerenter")
    eventProp[dom.PointerEvent]("pointerdown")
    eventProp[dom.PointerEvent]("pointermove")
    eventProp[dom.PointerEvent]("pointerup")
    eventProp[dom.PointerEvent]("pointercancel")
    eventProp[dom.PointerEvent]("pointerout")
    eventProp[dom.PointerEvent]("pointerleave")
    eventProp[dom.PointerEvent]("gotpointercapture")
    eventProp[dom.PointerEvent]("lostpointercapture")
    eventProp[dom.Event]("change")
    eventProp[dom.Event]("select")
    eventProp[dom.InputEvent]("beforeinput")
    eventProp[dom.Event]("input")
    eventProp[dom.FocusEvent]("blur")
    eventProp[dom.FocusEvent]("focus")
    eventProp[dom.Event]("submit")
    eventProp[dom.Event]("reset")
    eventProp[dom.Event]("invalid")
    eventProp[dom.Event]("search")
    eventProp[dom.KeyboardEvent]("keydown")
    eventProp[dom.KeyboardEvent]("keyup")
    eventProp[dom.KeyboardEvent]("keypress")
    eventProp[dom.ClipboardEvent]("copy")
    eventProp[dom.ClipboardEvent]("cut")
    eventProp[dom.ClipboardEvent]("paste")
    eventProp[dom.Event]("abort")
    eventProp[dom.Event]("canplay")
    eventProp[dom.Event]("canplaythrough")
    eventProp[dom.Event]("cuechange")
    eventProp[dom.Event]("durationchange")
    eventProp[dom.Event]("emptied")
    eventProp[dom.Event]("ended")
    eventProp[dom.Event]("loadeddata")
    eventProp[dom.Event]("loadedmetadata")
    eventProp[dom.Event]("loadstart")
    eventProp[dom.Event]("pause")
    eventProp[dom.Event]("play")
    eventProp[dom.Event]("playing")
    eventProp[dom.Event]("progress")
    eventProp[dom.Event]("ratechange")
    eventProp[dom.Event]("seeked")
    eventProp[dom.Event]("seeking")
    eventProp[dom.Event]("stalled")
    eventProp[dom.Event]("suspend")
    eventProp[dom.Event]("timeupdate")
    eventProp[dom.Event]("volumechange")
    eventProp[dom.Event]("waiting")
    eventProp[dom.AnimationEvent]("animationend")
    eventProp[dom.AnimationEvent]("animationiteration")
    eventProp[dom.AnimationEvent]("animationstart")
    eventProp[dom.UIEvent]("load")
    eventProp[dom.UIEvent]("resize")
    eventProp[dom.UIEvent]("scroll")
    eventProp[dom.Event]("show")
    eventProp[dom.Event]("toggle")
    eventProp[dom.Event]("transitionend")
    eventProp[dom.Event]("DOMContentLoaded")
    eventProp[dom.Event]("fullscreenchange")
    eventProp[dom.Event]("fullscreenerror")
    eventProp[dom.Event]("visibilitychange")
    eventProp[dom.Event]("afterprint")
    eventProp[dom.Event]("beforeprint")
    eventProp[dom.BeforeUnloadEvent]("beforeunload")
    eventProp[dom.HashChangeEvent]("hashchange")
    eventProp[dom.MessageEvent]("message")
    eventProp[dom.MessageEvent]("messageerror")
    eventProp[dom.Event]("offline")
    eventProp[dom.Event]("online")
    eventProp[dom.PageTransitionEvent]("pagehide")
    eventProp[dom.PageTransitionEvent]("pageshow")
    eventProp[dom.PopStateEvent]("popstate")
    eventProp[dom.StorageEvent]("storage")
    eventProp[dom.UIEvent]("unload")
    eventProp[dom.ErrorEvent]("error")
  }

  /** [[com.raquo.laminar.defs.eventProps.GlobalEventProps]]
    */
  trait GlobalEventProps { self: EventsDefine =>
    eventProp[dom.MouseEvent]("click")
    eventProp[dom.MouseEvent]("dblclick")
    eventProp[dom.MouseEvent]("mousedown")
    eventProp[dom.MouseEvent]("mousemove")
    eventProp[dom.MouseEvent]("mouseout")
    eventProp[dom.MouseEvent]("mouseover")
    eventProp[dom.MouseEvent]("mouseleave")
    eventProp[dom.MouseEvent]("mouseenter")
    eventProp[dom.MouseEvent]("mouseup")
    eventProp[dom.WheelEvent]("wheel")
    eventProp[dom.MouseEvent]("contextmenu")
    eventProp[dom.DragEvent]("drag")
    eventProp[dom.DragEvent]("dragend")
    eventProp[dom.DragEvent]("dragenter")
    eventProp[dom.DragEvent]("dragleave")
    eventProp[dom.DragEvent]("dragover")
    eventProp[dom.DragEvent]("dragstart")
    eventProp[dom.DragEvent]("drop")
    eventProp[dom.PointerEvent]("pointerover")
    eventProp[dom.PointerEvent]("pointerenter")
    eventProp[dom.PointerEvent]("pointerdown")
    eventProp[dom.PointerEvent]("pointermove")
    eventProp[dom.PointerEvent]("pointerup")
    eventProp[dom.PointerEvent]("pointercancel")
    eventProp[dom.PointerEvent]("pointerout")
    eventProp[dom.PointerEvent]("pointerleave")
    eventProp[dom.PointerEvent]("gotpointercapture")
    eventProp[dom.PointerEvent]("lostpointercapture")
    eventProp[dom.TouchEvent]("touchstart")
    eventProp[dom.TouchEvent]("touchmove")
    eventProp[dom.TouchEvent]("touchcancel")
    eventProp[dom.TouchEvent]("touchend")
    eventProp[dom.Event]("change")
    eventProp[dom.Event]("select")
    eventProp[dom.InputEvent]("beforeinput")
    eventProp[dom.Event]("input")
    eventProp[dom.FocusEvent]("blur")
    eventProp[dom.FocusEvent]("focus")
    eventProp[dom.Event]("submit")
    eventProp[dom.Event]("reset")
    eventProp[dom.Event]("invalid")
    eventProp[dom.Event]("search")
    eventProp[dom.KeyboardEvent]("keydown")
    eventProp[dom.KeyboardEvent]("keyup")
    eventProp[dom.KeyboardEvent]("keypress")
    eventProp[dom.ClipboardEvent]("copy")
    eventProp[dom.ClipboardEvent]("cut")
    eventProp[dom.ClipboardEvent]("paste")
    eventProp[dom.Event]("abort")
    eventProp[dom.Event]("canplay")
    eventProp[dom.Event]("canplaythrough")
    eventProp[dom.Event]("cuechange")
    eventProp[dom.Event]("durationchange")
    eventProp[dom.Event]("emptied")
    eventProp[dom.Event]("ended")
    eventProp[dom.Event]("loadeddata")
    eventProp[dom.Event]("loadedmetadata")
    eventProp[dom.Event]("loadstart")
    eventProp[dom.Event]("pause")
    eventProp[dom.Event]("play")
    eventProp[dom.Event]("playing")
    eventProp[dom.Event]("progress")
    eventProp[dom.Event]("ratechange")
    eventProp[dom.Event]("seeked")
    eventProp[dom.Event]("seeking")
    eventProp[dom.Event]("stalled")
    eventProp[dom.Event]("suspend")
    eventProp[dom.Event]("timeupdate")
    eventProp[dom.Event]("volumechange")
    eventProp[dom.Event]("waiting")
    eventProp[dom.AnimationEvent]("animationend")
    eventProp[dom.AnimationEvent]("animationiteration")
    eventProp[dom.AnimationEvent]("animationstart")
    eventProp[dom.Event]("transitionend")
    eventProp[dom.UIEvent]("load")
    eventProp[dom.UIEvent]("resize")
    eventProp[dom.UIEvent]("scroll")
    eventProp[dom.Event]("selectstart")
    eventProp[dom.Event]("show")
    eventProp[dom.Event]("toggle")
    eventProp[dom.ErrorEvent]("error")
  }

  /** com.raquo.laminar.defs.eventProps.WindowEventProps */
  trait WindowEventProps { self: EventsDefine =>
    eventProp[dom.Event]("afterprint")
    eventProp[dom.Event]("beforeprint")
    eventProp[dom.BeforeUnloadEvent]("beforeunload")
    eventProp[dom.HashChangeEvent]("hashchange")
    eventProp[dom.MessageEvent]("message")
    eventProp[dom.MessageEvent]("messageerror")
    eventProp[dom.Event]("offline")
    eventProp[dom.Event]("online")
    eventProp[dom.PageTransitionEvent]("pagehide")
    eventProp[dom.PageTransitionEvent]("pageshow")
    eventProp[dom.PopStateEvent]("popstate")
    eventProp[dom.StorageEvent]("storage")
    eventProp[dom.UIEvent]("unload")
  }
}
