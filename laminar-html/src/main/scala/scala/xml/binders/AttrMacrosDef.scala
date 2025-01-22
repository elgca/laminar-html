package scala.xml
package binders

import scala.quoted.*
import scala.xml.MacorsMessage.AttrType

trait AttrMacrosDef[R](using Quotes, Type[R], AttrType) {
  def checkType[T](using Type[T]): Boolean
  def supportSource: Boolean = true

  protected def block[T: Type](body: => Expr[MetatDataBinder])(using MacrosPosition): Expr[MetatDataBinder] = {
    if !checkType[T] then MacorsMessage.expectationType[T, R | Option[R]]
    MacorsMessage.showSupportedTypes[R | Option[R]]
    body
  }

  protected def withConstImpl[T: Type](
    namespace: Expr[NamespaceBinding => Option[String]],
    prefix: Option[String],
    attrKey: String,
    constStr: String,
  )(using MacrosPosition): Expr[MetatDataBinder]

  def withConst[T: Type](
    namespace: Expr[NamespaceBinding => Option[String]],
    prefix: Option[String],
    attrKey: String,
    constStr: String,
  )(using MacrosPosition): Expr[MetatDataBinder] = block[T](withConstImpl(namespace, prefix, attrKey, constStr))

  protected def withExprImpl[T: Type](
    namespace: Expr[NamespaceBinding => Option[String]],
    prefix: Option[String],
    attrKey: String,
    expr: Expr[T],
  )(using MacrosPosition): Expr[MetatDataBinder]

  def withExpr[T: Type](
    namespace: Expr[NamespaceBinding => Option[String]],
    prefix: Option[String],
    attrKey: String,
    expr: Expr[T],
  )(using MacrosPosition): Expr[MetatDataBinder] = block[T](withExprImpl(namespace, prefix, attrKey, expr))

  def withExprFromSourceImpl[V: Type, CC <: Source[V]: Type](
    namespace: Expr[NamespaceBinding => Option[String]],
    prefix: Option[String],
    attrKey: String,
    sourceValue: Expr[CC],
  )(using MacrosPosition): Expr[MetatDataBinder] = {
    '{ (ns: NamespaceBinding, element: ReactiveElementBase) =>
      ReactiveElement.bindFn(element, ${ sourceValue }.toObservable) { nextValue =>
        ${
          val updaterExpr: Expr[MetatDataBinder] = withExpr(
            namespace = namespace,
            prefix = prefix,
            attrKey = attrKey,
            expr = 'nextValue,
          )
          updaterExpr
        }.apply(ns, element)
      }
    }
  }

  def withExprFromSource[V: Type, CC <: Source[V]: Type](
    namespace: Expr[NamespaceBinding => Option[String]],
    prefix: Option[String],
    attrKey: String,
    sourceValue: Expr[CC],
  )(using MacrosPosition): Expr[MetatDataBinder] = block[V] {
    withExprFromSourceImpl(namespace, prefix, attrKey, sourceValue)
  }
}

type AttrMacrosDefFactory = (quotes: Quotes) => AttrMacrosDef[?]

object AttrMacrosDef {

  def unapply[T](
    tuple: (Option[String], String, Type[T]),
  )(using quotes: Quotes): Option[(String, AttrMacrosDef[?])] = {
    Events
      .unapply(tuple)
      .orElse(Attrs.unapply(tuple))
  }
}
