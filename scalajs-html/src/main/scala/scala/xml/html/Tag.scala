package scala.xml
package html

class Tag(val name: String)

object Tag {
  val svgTag                                         = Tag("svg")
  implicit inline def svg(inline name: "svg"): Tag   = svgTag
  implicit inline def rect(inline name: String): Tag = Tag(name)
//  val rectTag                                              = Tag("rect")
//  implicit inline def rect(inline name: "rect"): Tag       = rectTag
//  val animateTag                                           = Tag("animate")
//  implicit inline def animate(inline name: "animate"): Tag = animateTag
}
