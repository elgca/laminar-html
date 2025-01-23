package test

import org.scalajs.dom
import org.scalajs.dom.DragEvent

object EventOptions {

//
  val opts = {
//    import scala.xml.L.*
//    import com.raquo.laminar.api.features.unitArrows
//    scala.xml.L.onClick --> { println("hello") }
//    onClick.mapToValue
//    L
    <div
      onclick={(() => println("onclick"))}
      gggg="{}"
      ccc={(e: dom.DragEvent) => { println("ondrag event") }} 
    />
  }
}
