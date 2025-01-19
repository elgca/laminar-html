package scala.xml

import scala.util.NotGiven

class UnprefixedAttribute[V](
  key: String,
  value: V,
  next: MetaData,
)

object UnprefixedAttribute {

  implicit inline def toMetaData[V](
    inline data: UnprefixedAttribute[V],
  )(using inline ev: NotGiven[V <:< Source[?]]): MetaData = {
    MacrosTool.attribute(data)
  }

  implicit inline def toMetaData[CC[x] <: Source[x], V](
    inline data: UnprefixedAttribute[CC[V]],
  ): MetaData = {
    MacrosTool.attributeRx(data)
  }
}

class PrefixedAttribute[V](
  prefix: String,
  key: String,
  value: V,
  next: MetaData,
)

object PrefixedAttribute {

  implicit inline def toMetaData[V](
    inline data: PrefixedAttribute[V],
  )(using inline ev: NotGiven[V <:< Source[?]]): MetaData = {
    MacrosTool.attribute(data)
  }

  implicit inline def toMetaData[CC[x] <: Source[x], V](
    inline data: PrefixedAttribute[CC[V]],
  ): MetaData = {
    MacrosTool.attributeRx(data)
  }
}
