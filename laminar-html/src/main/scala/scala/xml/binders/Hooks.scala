package scala.xml
package binders



object Hooks {
  def unapply(e: String): Option[String] = {
    hooks.find(key => key.equalsIgnoreCase(e))
  }

  val hooks = Set(
    "onunmount",
    "onmount",
  )
}
