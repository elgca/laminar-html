# Laminar-html

[English](readme_en.md)

为Laminar提供 XHTML 语法支持, 通过scala的xml字面量创建Laminar节点. 

注意: 
1. 该库跟scala-xml是不兼容的
2. html允许一些标签不需要手动闭合, 但是XML要求所有标签必须闭合

Invalid:

```xhtml
<br>
<img src="image.jpg" alt="image">
<input type="text" name="username">
```

Valid:

```xhtml
<br />
<img src="image.jpg" alt="示例图片" />
<input type="text" name="username" />
```


# 使用

```scala
"io.github.elgca" %%% "laminar-html" % "0.2.1"
```

- 具备跟Laminar的完全互操作性
  - 可以将XHTML、Laminar自由的组合在一起:
    - `<button> {L.onClick --> count.update(_ + 1)} </button>`
    - `L.div(<button onclick={() => println("clicked")} />)`
    - `<div>{L.button(L.onClick --> println("clicked"))}</div>`
- Var[x]/Source[x]存储状态事件, 可以作为属性/子节点绑定到xhtml

```scala
val xhtmlElem = {
  val count = Var(0)
  <div>
    <h1 class="title">Hello World</h1>
    <button 
      class="btn btn-primary"
      onclick={() => count.update(_ + 1) }
    >
      xHtml Button
    </button>
    <p>Count: {count}</p> {/* Source[x] use as a child node */}
    {
      // this is a laimianr node.This is great, isn't it?
      L.button(
        className := "btn btn-primary",
        onClick --> count.update(_ + 1),
        "Laminar Button"
      )
    }
  </div>
}

val laminarElem = {
  L.div(
    "laminar element",
    xhtmlElem 
  )
}

// L.renderOnDomContentLoaded(document.getElementById("app"), xhtmlElem)
L.renderOnDomContentLoaded(document.getElementById("app"), laminarElem)
```

## 编译时类型检查校验

- 0.2.0开始,使用scala3的宏对属性节点进行处理,编译期间检查Events、htmlProp、htmlAttr参数类型
- 根据属性key提供更精确的类型类型判断, 例如: value只接收string,checked只接收bool
- 关闭/开启类型提示: build.sbt中配置 `val _ = System.setProperty("show_type_hints", "true")`

异常类型提示:
![img.png](images/img.png)

类型信息提示:
![typehints.png](images/typehints.png)![typeinfo.png](images/typeinfo.png)


## onmount/onunmount生命周期事件属性 

属性名不区分大小写,所以可以写如果愿意可以使用例如: 
onMount/onUnmount

```scala
val element = () => {
  val chart:Option[Chart] = None
  <div
      class="container"
      onmount={(ref:dom.Element) => {chart = initChart(ref)} }
      onunmount={() => chart.foreach(_.destory())}
  />
}
```


# 允许作为子节点嵌入

1. RenderableNode, 可渲染的组件
   1. 所有基础数据类型以及java.number.Number, 将会以_.toString的方式作为TextNode插入
   2. Laminar的ChildNodeBase
   3. xml节点
   4. 以上类型的Union类型
   4. 任意的Laminar Modifier
   5. Tuple,`Tuple.Union <: [RenderableNode|Modifier|Seq[...]]`
5. `IterableOnce[CC]`:
   1. `Option[RenderableNode/Modifier]`
   7. `Seq[RenderableNode/Modifier]`
   8. ...
9. `Var/Signal`
   1. `Source[RenderableNode]`
   11. `Source[Collection[RenderableNode]]`,
       12. `Collection <: Seq | Array| js.Array| ew.JsArray | ew.JsVector | laminar.Seq`
   13. `Source[Option[RenderableNode]]`
   14. `js.Promise[RenderableNode]`
1. 使用 js.dynamicImport时候很有用, 可以允许你先完成其他部分,当加载完成后再挂载

# 自定义属性处理

给定隐式 `UserDefinedAttributeHandler[PropName,DataType]` 实现可以自定义属性的处理逻辑

```scala 3
given UserDefinedAttributeHandler["ggccf", String] with
  override def withValue(....):Unit = {...}
  override def withSourceValue(...):Unit = {...}
```

![udattr.png](images/udattr.png)

# 可选编译配置

编译配置可以通过sbt中添加`System.setProperty("show_type_hints", "true")`设置

- show_type_hints
  - default true
  - 是否显示类型提示,由report.info提供
- strict_event_function 
  - default true
  - 启用严格函数校验
  - 例如:onclick不能接受类似`(dom.FetchEvent) => Unit`,因为`dom.FetchEvent`无法被视作`dom.MouseEvent`