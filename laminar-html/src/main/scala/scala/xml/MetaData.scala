package scala.xml

trait MetaData {
  def foreach(f: MetaData => Unit): Unit

  def next: MetaData

  def apply(namespaceBinding: NamespaceBinding, element: ReactiveElementBase): Unit
}

object MetaData {
  val EmptyBinder: (namespaceBinding: NamespaceBinding, element: ReactiveElementBase) => Unit = (a, b) => {}

  class Attribute(
    val binder: (namespaceBinding: NamespaceBinding, element: ReactiveElementBase) => Unit,
    val next: MetaData,
  ) extends MetaData {

    def foreach(f: MetaData => Unit): Unit = {
      f(this)
      next.foreach(f)
    }

    def apply(namespaceBinding: NamespaceBinding, element: ReactiveElementBase): Unit =
      binder(namespaceBinding, element)
  }

  inline def apply(
    inline binder: (namespaceBinding: NamespaceBinding, element: ReactiveElementBase) => Unit,
    inline next: MetaData): MetaData = {
    new Attribute(binder, next)
  }
}

case object Null extends MetaData {
  override def next: MetaData = this

  override def foreach(f: MetaData => Unit): Unit = {}

  def apply(namespaceBinding: NamespaceBinding, element: ReactiveElementBase): Unit = {}
}
