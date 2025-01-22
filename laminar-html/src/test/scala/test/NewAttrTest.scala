package test

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.*

import scala.xml.binders.Attrs.AttrProvider

//import scala.xml.AttrKey.given
import com.raquo.laminar.api.features.unitArrows
given bcs: AttrProvider["bcs"] = null.asInstanceOf[AttrProvider["bcs"]]

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
