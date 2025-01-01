package scala.xml

import com.raquo.laminar.tags.Tag
import org.scalajs.dom

import scala.xml.Elem.ElemTag

class Elem(
  override val tag: ElemTag,
  override val ref: dom.Element,
) extends ElementNodeBase derives CanEqual {
  def this(
    tagName: ElemTag,
    prefix: String | scala.Null,
    scope: NamespaceBinding,
  ) = {
    this(tagName, Elem.createElement(tagName, prefix, scope))
  }

  def this(
    prefix: String | scala.Null,
    tagName: String,
    attributes: MetaData,
    scope: NamespaceBinding,
    minimizeEmpty: Boolean,
    child: Node*,
  ) = {
    this(ElemTag(tagName), prefix, scope)
    attributes.foreach(attrSetter => attrSetter(scope, this))
    child.foreach(child => child.apply(this))
  }

}

object Elem {

  class ElemTag(
    val name: String,
    val void: Boolean = false,
  ) extends Tag[ElementNodeBase] derives CanEqual {
    override def jsTagName: String = name
  }

  def createElement(tagName: ElemTag, prefix: String | scala.Null, scope: NamespaceBinding): dom.Element = {
    scope.namespaceURI(prefix) match
      case Some(ns) => dom.document.createElementNS(ns, tagName.name)
      case None     => dom.document.createElement(tagName.name)
  }
}
