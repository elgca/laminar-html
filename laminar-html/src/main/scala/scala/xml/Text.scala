package scala.xml

/** XML node for text */
class Text(val data: String) extends AnyVal{
  override def toString: String = data
}

object Text {
  def apply(data: String): Text = new Text(data)

  def unapply(other: Any): Option[String] = other match {
    case x: Text => Some(x.data)
    case _       => None
  }
}
