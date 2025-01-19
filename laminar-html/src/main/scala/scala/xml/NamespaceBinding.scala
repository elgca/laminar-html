package scala.xml

class NamespaceBinding(
  val prefix: String | scala.Null,
  val uri: String | scala.Null,
  val next: NamespaceBinding | scala.Null,
) {

  infix def namespaceURI(prefix: String | scala.Null): Option[String] =
    if prefix == this.prefix then uri.toOption else next.toOption.flatMap(_ namespaceURI prefix)
}

case object TopScope extends NamespaceBinding(null, null, null) {
  final val svgNamespaceUri: String   = "http://www.w3.org/2000/svg"
  final val xlinkNamespaceUri: String = "http://www.w3.org/1999/xlink"
  final val xmlNamespaceUri: String   = "http://www.w3.org/XML/1998/namespace"
  final val xmlnsNamespaceUri: String = "http://www.w3.org/2000/xmlns/"
  // xml or base
  final val xhtmlNamespaceUri: String = "http://www.w3.org/1999/xhtml"
  final val mathNamespaceUri: String  = "http://www.w3.org/1998/Math/MathML"

  val namespaceUri: PartialFunction[String | scala.Null, String] = {
    case "svg"   => svgNamespaceUri
    case "xlink" => xlinkNamespaceUri
    case "xml"   => xmlNamespaceUri
    case "xmlns" => xmlnsNamespaceUri
    case "math"  => mathNamespaceUri
//    case "xhtml" | "" | null => xhtmlNamespaceUri // 需要这个么?
  }

  override def namespaceURI(prefix: String | scala.Null): Option[String] = namespaceUri.lift(prefix)

}
