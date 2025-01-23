package scala.xml.udf

import com.raquo.laminar.api.L.Source
import com.raquo.laminar.nodes.ReactiveElement

/** UserDefinedAttributeHandler
  * @tparam PropName
  *   StringConstType, attribuename
  * @tparam DataType
  *   except type
  */
trait UserDefinedAttributeHandler[PropName <: String, DataType] {

  // conversion const value to type
  def encode(constValue: String): DataType

  def withValue(
    namespaceURI: Option[String],
    namespacePrefix: Option[String],
    attrName: String,
    value: DataType,
    element: ReactiveElement.Base,
  ): Unit

  def withSourceValue(
    namespaceURI: Option[String],
    namespacePrefix: Option[String],
    attrName: String,
    sourceValue: Source[DataType],
    element: ReactiveElement.Base,
  ): Unit = {
    ReactiveElement.bindFn(element, sourceValue.toObservable) { nextValue =>
      withValue(
        namespaceURI = namespaceURI,
        namespacePrefix = namespacePrefix,
        attrName = attrName,
        value = nextValue,
        element = element,
      )
    }
  }
}
