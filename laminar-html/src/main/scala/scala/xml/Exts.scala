package scala.xml

extension [A](value: A | scala.Null) {
  def toOption: Option[A] = if value == null then None else Some(value)
}
