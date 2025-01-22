package test

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.eventPropToProcessor

import scala.scalajs.js
val boolVar = Var(false)
import com.raquo.laminar.api.L
import com.raquo.laminar.api.features.unitArrows

val app = {
  val value    = Var(0)
  val hidden   = true
  val dsfdsfsd = Option("true")
  <div class="test test1">
    <button onclick={() => value.update(_ + 1)}
            cccc={L.className := "htllo"}
            onmount={() => {}}
    >
      click me
    </button>
    <button onclick={(str: Boolean) => println(str)}
      hidden={hidden}
    >
      click me
    </button>
    <p content={dsfdsfsd}>count :{Seq("a")}</p>
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
    guo ="???"
    minLength ="1"
    width="121"
    onclick={clickFunc}
    >
  {L.onClick --> println("click-------->")}
  </button>
}

val svgHtml = {
  val classDef  = Option("bi bi-0-circle-fill")
  val classDef2 = Option("bi bi-0-circle-fill")
  <div>
    <button class={classDef2}>Click Me</button>
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="16"
        height="16"
        fill="currentColor"
        class={classDef}
        viewBox="0 0 16 16"
      >
        <path d="M8 4.951c-1.008 0-1.629 1.09-1.629 2.895v.31c0 1.81.627 2.895 1.629 2.895s1.623-1.09 1.623-2.895v-.31c0-1.8-.621-2.895-1.623-2.895"/>
        <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0m-8.012 4.158c1.858 0 2.96-1.582 2.96-3.99V7.84c0-2.426-1.079-3.996-2.936-3.996-1.864 0-2.965 1.588-2.965 3.996v.328c0 2.42 1.09 3.99 2.941 3.99"/>
      </svg>
  </div>
}

val subChild = {
  <div>
    {(<br />)}
  </div>

//  import com.raquo.laminar.api.*
//
//  iterableOnce(js.Array("items"))
//  val seq = Seq("a", L.div(), "c")
//  val seqCh = Var("")
//  L.div(
//    seq,
//  )
//  buf.&+(value)
//  val c: String = ""
//  <div>
//    {value}
//    {value2}
//  </div>
}
