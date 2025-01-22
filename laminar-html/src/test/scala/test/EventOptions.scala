package test

import org.scalajs.dom

object EventOptions {

  val opts = CompileCheck {
//    scala.xml.L.onClick --> { (e: dom.Event) => println("clicked") }

    <div
      onclick={(() => println("onclick"))}
    >
      <option value="" style="display:block"/>
    </div>
  }
}
