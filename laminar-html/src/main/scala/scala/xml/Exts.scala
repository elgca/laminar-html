package scala.xml

extension [A](value: A | scala.Null) {
  inline def toOption: Option[A] = if value == null then None else Some(value)
}
