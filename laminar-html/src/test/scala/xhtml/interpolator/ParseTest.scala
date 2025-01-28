package xhtml.interpolator

import _root_.test.CompileCheck
import com.raquo.airstream.state.Var
import utest.*
import xhtml.html

object ParseTest {

//  given Reporter = new Reporter {
//
//    def error(msg: String, idx: Int): Nothing = {
//      throw new IllegalAccessError(s"${msg}:${idx}")
//    }
//
//    def error(msg: String, expr: Expr[Any]): Nothing =
//      throw new IllegalAccessError(msg)
//  }

  val tests = Tests {
    test("") {
      CompileCheck {
        val cnt          = Var(0)
        val onclick      = () => cnt.update(_ + 1)
        val reset        = () => cnt.update(_ => 0)
        //        val c = summon[AcceptableNode["hello"]]
        //        println(c)
        //        val _ = CompileCheck("========>" + c)
        val cntttttttttt = Var(0)
        html"""
          <div>
            <button
              onclick=${onclick}
            >
              click me: ${cnt}
            </button>
            <button onclick=${reset} />
            <img src="test/uri"  alt="image">
          </div>
           """
      }
    }
  }
}
