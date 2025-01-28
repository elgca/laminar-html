package xhtml
package interpolator

import scala.quoted.*

class XmlContext(val args: Seq[Expr[Any]], val scope: Expr[Scope])
