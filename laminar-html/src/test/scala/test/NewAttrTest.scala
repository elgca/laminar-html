package test

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.features.unitArrows

object NewAttrTest {

  def test = CompileCheck {
    val func = Var(() => println("Hello world"))
    div(<button onclick={func} />)
    <div>{
      button(onClick --> {
        println("clicked")
      })
    }
    </div>

  }
}
