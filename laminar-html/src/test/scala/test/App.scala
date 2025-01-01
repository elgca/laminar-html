package test

import com.raquo.airstream.state.Var
import scala.xml.RenderableNodeImplicit.given

val app = {
  val value = Var(0)
  <div>
    <button click={() => value.update(_ + 1)}>
      click me
    </button>
    <p>count :{value}</p>
  </div>
}
