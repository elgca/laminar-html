package scala.xml
package binders

object LaminarModApi {

  def bindMod(mod: LModBase): MetatDataBinder = (ns, element) => {
    mod.apply(element)
  }
}
