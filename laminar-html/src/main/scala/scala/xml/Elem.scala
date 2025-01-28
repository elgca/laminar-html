package scala.xml

import org.scalajs.dom
import org.scalajs.dom.svg.Element

class Elem(
  val reactiveElement: ReactiveElementBase,
  //  override val tag: ElemTag,
  //  override val ref: dom.Element,
) extends Node derives CanEqual {
  def this(
    tagName: String,
    prefix: String | scala.Null,
    scope: NamespaceBinding,
  ) = {
    this(Elem.createReactiveElement(tagName, prefix, scope))
  }

  def this(
    prefix: String | scala.Null,
    tagName: String,
    attributes: MetaData,
    scope: NamespaceBinding,
    minimizeEmpty: Boolean,
    child: Node*,
  ) = {
    this(tagName, prefix, scope)
    attributes.foreach(attrSetter => attrSetter(scope, reactiveElement))
    child.foreach(child => child.apply(reactiveElement))
  }

  // append to parent
  override def apply(element: ReactiveElementBase): Unit =
    this.reactiveElement.apply(element)
}

object Elem {

  inline given Conversion[Elem, ReactiveElementBase] with
    inline def apply(elem: Elem): ReactiveElementBase = elem.reactiveElement

  given int: LaminarRenderableNode[Elem] = LaminarRenderableNode(x => x.reactiveElement)

  def createElement(tagName: String, prefix: String | scala.Null, scope: NamespaceBinding): dom.Element = {
    scope.namespaceURI(prefix) match
      case Some(ns) => dom.document.createElementNS(ns, tagName)
      case None     => dom.document.createElement(tagName)
  }

  def createReactiveElement(
    tagName: String,
    prefix: String | scala.Null,
    scope: NamespaceBinding): ReactiveElementBase = {
    scope.namespaceURI(prefix) match
      case Some(ns) if ns == TopScope.svgNamespaceUri =>
        new ReactiveSvgElement(
          new SvgTag(tagName),
          dom.document.createElementNS(ns, tagName).asInstanceOf[dom.svg.Element],
        )
      case Some(ns)                                   =>
        // 这里我应该使用ReactiveHtmlElement?或者我应该用ElementNodeBase?
        // ElementNodeBase将无法嵌入Laminar的HtmlProp等,因为类型不匹配
        // 但是dom.document.createElementNS返回的是dom.html.Element么?
        new ReactiveHtmlElement(
          new HtmlTag(tagName),
          dom.document.createElementNS(ns, tagName).asInstanceOf[dom.html.Element],
        )
      case None                                       =>
        new ReactiveHtmlElement(
          new HtmlTag(tagName),
          dom.document.createElement(tagName).asInstanceOf[dom.html.Element])
  }
}
