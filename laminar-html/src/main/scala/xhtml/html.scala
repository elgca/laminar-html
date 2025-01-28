package xhtml

private[xhtml] type Scope = scala.xml.NamespaceBinding

/** TODO: THIS NEED TO BE DOCUMENTED */
extension (inline ctx: StringContext)

  transparent inline def html(inline args: Any*)(using inline scope: Scope): Any =
    ${ interpolator.Macro.impl('ctx, 'args, 'scope) }
