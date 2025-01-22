package scala.xml.binders

extension [T](it: Iterator[T]) {

  def headOrMatch(f: T => Boolean): Option[T] = {
    var head = Option.empty[T]
    var res  = Option.empty[T]
    it.exists(r => {
      if head.isEmpty then head = Some(r)
      if f(r) then res = Some(r)
      res.isDefined
    })
    res.orElse(head)
  }
}
