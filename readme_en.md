# Laminar-html

[中文](readme.md)

Provides XHTML syntax support for Laminar, creating Laminar nodes through Scala's XML literals.

Note: 
1. This library is incompatible with scala-xml
2. HTML allow some unclosed tags, but XML/XHTML syntax must close all tag




# example

Sometimes, the copied HTML may contain errors,
usually due to the strict XML/XHTML syntax.
These errors often stem from unclosed tags.

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

start

```shell

cd example
sbt ~fastLinkJS
npm install
npm run dev
```


# usage

```scala
"io.github.elgca" %%% "laminar-html" % "0.1.6"
```

- fully interoperable with Laminar.
  - You can freely combine XHTML and Laminar:
    - `<button> {L.onClick --> count.update(_ + 1)} </button>`
    - `L.div(<button onclick={() => println("clicked")} />)`
    - `<div>{L.button(L.onClick --> println("clicked"))}</div>`
- Var[x]Source[x] stores state events and can be bound to xhtml as property children

Example 1:

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

## compile time type check validation

- Since 0.2.0, scala3 macros are used to process property nodes, and the parameter types of Events, htmlProp, and htmlAttr are checked during compilation
- It provides more accurate type type judgment based on the attribute key, for example: value only receives string, checked only receives bool
- Open/Close Type Hints: `build.sbt` add `val _ = System.setProperty("show_type_hints", "true")`
- Hints language support Chinese/English

error type hints:
![img.png](images/img.png)

type hints info:
![typeinfo.png](images/typeinfo.png)


## onmount/onunmount Lifecycle hooks

Property names are not case-sensitive, 
so you can write them if you want to use e.g.
- onMount / mount
- onUnmount / unmount

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

