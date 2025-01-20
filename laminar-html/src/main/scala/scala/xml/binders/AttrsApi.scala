package scala.xml
package binders

import com.raquo.ew.ewArray
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.JSStringOps.*

object AttrsApi {

  def setHtmlAttributeBinder(
    namespace: NamespaceBinding => Option[String],
    name: String,
    value: String | Null,
  ): MetatDataBinder = (binding: NamespaceBinding, element: ReactiveElementBase) => {
    setHtmlAttributeRaw(namespace(binding), element.ref, name, value)
  }

  def getHtmlAttributeRaw(
    namespaceURI: Option[String],
    element: dom.Element,
    attrName: String,
  ): js.UndefOr[String] = {
    val domValue: String | Null = element.getAttributeNS(
      namespaceURI = if namespaceURI.isEmpty then null.asInstanceOf[String] else namespaceURI.get,
      localName = attrName,
    )
    if domValue != null then {
      domValue
    } else {
      js.undefined
    }
  }

  def setHtmlAttributeRaw(
    namespaceURI: Option[String],
    element: dom.Element,
    name: String,
    value: String | Null,
  ): js.UndefOr[String] = {
    if value == null || value.isEmpty then {
      if namespaceURI.isEmpty then element.removeAttribute(name)
      else element.removeAttributeNS(namespaceURI.get, name)
    } else {
      if namespaceURI.isEmpty then element.setAttribute(name, value.asInstanceOf[String])
      else element.setAttributeNS(namespaceURI.get, name, value.asInstanceOf[String])
    }
  }

  def setCompositeAttributeBinder(
    namespace: (NamespaceBinding) => Option[String],
    name: String,
    itemsToAdd: List[String],
    itemsToRemove: List[String],
  ): MetatDataBinder = (binding: NamespaceBinding, element: ReactiveElementBase) => {
    val namespaceURI = namespace(binding)

    val separator = " "
    val domValue  = getHtmlAttributeRaw(namespaceURI, element.ref, name)
      .map(CompositeNormalize.normalize(_, separator))
      .getOrElse(Nil)
    val newItems  = (domValue.filterNot(t => itemsToRemove.contains(t)) ++ itemsToAdd).distinct

    val nextDomValue = newItems.mkString(separator)
    setHtmlAttributeRaw(namespaceURI, element.ref, name, nextDomValue)
  }

  trait CompositeNormalize[T] {
    def apply(items: T): List[String]
  }

  object CompositeNormalize {
    type CompositeValidTypes = String | IterableOnce[String]

    /** @param items
      *   non-normalized string with one or more items
      *   separated by `separator`
      * @return
      *   individual values. Note that normalization does
      *   NOT ensure that the items are unique.
      */
    def normalize(items: String, separator: String): List[String] = {
      if items.isEmpty then {
        Nil
      } else {
        items.jsSplit(separator).ew.filter(_.nonEmpty).asScalaJs.toList
      }
    }

    given str: CompositeNormalize[String] = items => normalize(items, " ")

    given seq[CC <: IterableOnce[String]]: CompositeNormalize[CC] = value =>
      value.iterator.flatMap(items => normalize(items, " ")).toList
  }

  // namespace provider
  val noNamespace                                    = (ns: NamespaceBinding) => None
  def namespaceWithPrefix(prefix: String)            = (ns: NamespaceBinding) => ns.namespaceURI(prefix)
  def namespaceWithURI(namespaceURI: Option[String]) = (ns: NamespaceBinding) => namespaceURI
}
