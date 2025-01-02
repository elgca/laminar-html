package test

import com.raquo.airstream.state.Var
import scala.xml.RenderableNodeImplicit.given

val app = CompileCheck {
  //
  val value = Var(0)
  <div>
    <button click={() => value.update(_ + 1)}>
      click me
    </button>
    <button click={(str: Boolean) => println(str)}>
      click me
    </button>
    <p>count :{value}</p>
  </div>
}
