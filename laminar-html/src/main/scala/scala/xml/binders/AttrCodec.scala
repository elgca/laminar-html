package scala.xml.binders

//type AttrCodec[ScalaType, DomType] = com.raquo.laminar.codecs.Codec[ScalaType, DomType]
// 我希望能够自动寻找隐式定义, 放弃 com.raquo.laminar.codecs.Codec[ScalaType, DomType]
trait AttrCodec[ScalaType, DomType] {
  def decode(domValue: DomType): ScalaType
  def encode(scalaValue: ScalaType): DomType
}

object AttrCodec {

  given AttrCodec[String, Boolean] = new AttrCodec[String, Boolean] {
    override def decode(domValue: Boolean): String = if domValue then "true" else "false"

    override def encode(scalaValue: String): Boolean = if scalaValue == "true" then true else false
  }
}
