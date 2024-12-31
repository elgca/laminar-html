package scala.xml

class Elem extends Node {
  def this(
    prefix: String | Null,
    label: html.Tag,
    attributes: MetaData,
    scope: NamespaceBinding,
    minimizeEmpty: Boolean,
    child: Node*,
  ) = {
    this()
  }
}
