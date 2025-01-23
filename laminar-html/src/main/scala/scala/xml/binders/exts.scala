package scala.xml.binders

import scala.collection.IterableOnce

extension [T](it: IterableOnce[T]) {

  def headOrMatch(f: T => Boolean): Option[T] = {
    var head = Option.empty[T]
    var res  = Option.empty[T]
    it.iterator.exists(r => {
      if head.isEmpty then head = Some(r)
      if f(r) then res = Some(r)
      res.isDefined
    })
    res.orElse(head)
  }
}
