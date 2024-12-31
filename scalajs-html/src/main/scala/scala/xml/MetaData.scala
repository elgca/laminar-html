package scala.xml

trait MetaData

case object Null extends MetaData

class UnprefixedAttribute[T] extends MetaData {
  def this(key: String, value: T, next: MetaData) = this()
}

//object UnprefixedAttribute {
//  def unapply[T](x: UnprefixedAttribute[T]): Some[(String, T, MetaData)] = Some((x.key, x.value, x.next))
//}

class PrefixedAttribute[T] extends MetaData {
  def this(
    prefix: String,
    key: String,
    value: T,
    next: MetaData,
  ) = this()
}

//object PrefixedAttribute {
//
//  def unapply[T](x: PrefixedAttribute[T]): Some[(String, String, T, MetaData)] = Some(
//    (x.prefix, x.key, x.value, x.next))
//}
