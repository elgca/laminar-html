package test

object NullNode {

  val nullNode = CompileCheck {
    <div
      class="container"
      onclick={() => println("clicked")}
      value=""
    >
      {println("hello, world")}
      {null}
    </div>
  }
}
