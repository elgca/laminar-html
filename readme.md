# Laminar-html

为Laminar提供 XHTML 语法支持, 通过scala的xml字面量创建Laminar节点. 

注意: 该库跟scala-xml是不兼容的

# 使用

- 所有的节点都是Laminar的ReactiveElement.Base所以具备跟Laminar的完全互操作性.
- 可以使用Laminar中所有的属性、事件和子节点, 例如: `<button> {L.onClick --> count.update(_ + 1)} </button>`
- 支持Airstream中所有的反应式变量
- 你可以把xhtml嵌入到Laminar的节点中或者把Laminar的节点嵌入到xhtml中

```scala
val xmlElem = {
  val count = Var(0)
  <div>
    <h1 class="title">Hello World</h1>
    <button 
      class="btn btn-primary"
      click={() => count.update(_ + 1) }
    >
      <!-- 事件函数会调用dom的addEventListener添加事件监听-->
      <!-- 参考 https://developer.mozilla.org/zh-CN/docs/Web/API/Element/click_event-->
      Html Button
    </button>
    <p>Count: {text <-- count}</p>
    L.button( // 在xml中嵌入Laminar节点
      className :="btn btn-primary",
      onClick --> count.update(_ + 1),
      "Laminar Button"
    )
  </div>
}

val laminarElem = {
  L.div(
    "laminar element",
    xmlElem // 把xml嵌入到laminar中
  )
}

L.renderOnDomContentLoaded(document.getElementById("app"), laminarElem)
```
