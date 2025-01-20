package example

import com.raquo.laminar.api.L
import org.scalajs.dom
import scala.language.implicitConversions

@main def main(): Unit = {
  L.renderOnDomContentLoaded(dom.document.getElementById("app"), page)
}
