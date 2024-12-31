package scala.xml

class NamespaceBinding(
  val prefix: String | Null,
  val uri: String | Null,
  val next: NamespaceBinding | Null,
) {
  def isEmpty = false
  def get     = this

  def _1 = prefix
  def _2 = uri
  def _3 = next

  infix def namespaceURI(prefix: String): Option[String] =
    if (prefix == this.prefix) Some(uri) else next namespaceURI prefix
}

object NamespaceBinding {
  def unapply(s: NamespaceBinding): NamespaceBinding = s
}

case object TopScope extends NamespaceBinding(null, null, null):
  override def namespaceURI(prefix: String): Option[String] = None
