package test

import com.raquo.laminar.nodes.ReactiveElement

import scala.xml.udf.UserDefinedAttributeHandler

object UdfProvider {

//
  given UserDefinedAttributeHandler["ggccf", Boolean] with

    def withValue(
      namespaceURI: Option[String],
      namespacePrefix: Option[String],
      attrName: String,
      value: Boolean,
      element: ReactiveElement.Base,
    ): Unit = {}

  val c = {
    <div ggccf={true}
    />
  }
}
