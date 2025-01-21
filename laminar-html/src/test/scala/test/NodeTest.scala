package test

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar

object NodeTest {

  val test = {
    val cnt = Var(0)
    <div>
      {(1, 2, <br/>, L.div())}
      {L.div("---------------------")}
      {cnt}
      {Option("GGGGGGGGGGGGG")}
    </div>
  }

  val test2 = {
    val cnt         = Var(0)
    val varString   = Var("test")
    val varSeq      = Var[Seq[String | scala.xml.Elem | Int]](Seq("0"))
    val lllllllllll = Var(L.div())
    <div>
      {cnt}
      {Option("GGGGGGGGGGGGG")}
      {lllllllllll}
      {varString}
      {varSeq}
    </div>
  }
}
