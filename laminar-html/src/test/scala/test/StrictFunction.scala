package test

import org.scalajs.dom.*

object StrictFunction {

  val opts = CompileCheck {
     <div
       gggg="{}"
       onclick={
         //      import org.scalajs.dom
         //        val fun = (e: DragEvent) => { println("ondrag event") } //in strict mode this is not valid
         val fun = (e: MouseEvent) => {}
         fun
       }
     />
  }
}
