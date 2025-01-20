package test

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.*

import scala.xml.RenderableNodeImplicit.given
val boolVar = Var(false)
import com.raquo.laminar.api.L

val test = {
  L.widthAttr
  L.width
  L.svg.width
//  L.className
  L.svg.className
  L.role
}

val app = {
  val value    = Var(0)
  val hidden   = true
  val dsfdsfsd = Option("true")
  <div class="test test1">
    <button onClick={() => value.update(_ + 1)}
            cccc={L.className := "htllo"}
            onmount={() => {}}
            ggg={value.signal.map(_.toString)}
    >
      click me
    </button>
    <button click={(str: Boolean) => println(str)}
      hidden={hidden}
    >
      click me
    </button>
    <p content={dsfdsfsd}>count :{value}</p>
    <test width ="300"></test>
  </div>
}

val test3 = {
  L.contentEditable := true
}

val testVar = {
  val value     = Var("0")
  val clickFunc = Var(() => println("click"))
  L.className := "hwiden"
  val attr = true
  <button
    value={value}
    contenteditable={"off"}
    click={clickFunc}
    />
}

val svgHtml = CompileCheck {
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="16"
    height="16"
    fill="currentColor"
    class="bi bi-0-circle-fill"
    viewBox="0 0 16 16"
  >
    <path d="M8 4.951c-1.008 0-1.629 1.09-1.629 2.895v.31c0 1.81.627 2.895 1.629 2.895s1.623-1.09 1.623-2.895v-.31c0-1.8-.621-2.895-1.623-2.895"/>
    <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0m-8.012 4.158c1.858 0 2.96-1.582 2.96-3.99V7.84c0-2.426-1.079-3.996-2.936-3.996-1.864 0-2.965 1.588-2.965 3.996v.328c0 2.42 1.09 3.99 2.941 3.99"/>
  </svg>
}
