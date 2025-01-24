package scala.xml.binders

extension [T](it: Iterable[T]) {

  inline def headOrMatch(f: T => Boolean): Option[T] = {
    it.find(x => f(x)).orElse(it.headOption)
  }
}
