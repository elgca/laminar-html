package test

import org.scalajs.dom

object EventOptions {

//
  val opts = CompileCheck {
    import scala.xml.L.*
    import com.raquo.laminar.api.features.unitArrows
//    scala.xml.L.onClick --> { println("hello") }
    //
    <div
      onclick={(() => println("onclick"))}
    />
  }
}
