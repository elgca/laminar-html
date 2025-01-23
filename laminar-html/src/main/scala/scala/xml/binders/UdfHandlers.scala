package scala.xml
package binders

import com.raquo.laminar.api.L.a

import scala.quoted.*
import scala.xml.MacorsMessage.AttrType
import scala.xml.{MacrosPosition, NamespaceBinding}
import scala.xml.udf.UserDefinedAttributeHandler

object UdfHandlers {

  def unapply[T](
    tuple: (Option[String], String, Type[T]),
  )(using quotes: Quotes): Option[(String, AttrMacrosDef[?], Seq[(String, String)])] = {
    val (prefix: Option[String], attrKey: String, tpe: Type[T]) = tuple
    import quotes.*
    import quotes.reflect.*

    val name = prefix.map(_ + ":").getOrElse("") + attrKey

    for
      hType     <- TypeRepr.of[UserDefinedAttributeHandler[?, ?]] match {
                     case AppliedType(tpe, _) => Some(tpe)
                     case _                   => None
                   }
      keyType    = ConstantType(StringConstant(name))
      handleType = AppliedType(hType, List(keyType, TypeRepr.of(using tpe))).asType
      impl      <- Implicits.search(TypeRepr.of(using handleType)) match {
                     case iss: ImplicitSearchSuccess => Some(iss.tree.asExpr)
                     case isf: ImplicitSearchFailure => None
                   }
      macros    <- handleType match {
                     case '[UserDefinedAttributeHandler[a, b]] =>
                       given attrType: AttrType =
                         AttrType(s"UserDefinedAttribute:<${name}>")
                       val handlerExpr          = impl.asInstanceOf[Expr[UserDefinedAttributeHandler[a, b]]]
                       val hdlMacros            = UdfHandlersMacros[a, b](handlerExpr)
                       Some((attrKey, hdlMacros, Seq(hdlMacros.supportedTypesMessage)))
                     case _                                    => None
                   }
    yield macros
  }

  class UdfHandlersMacros[PropName <: String, DataType](
    handler: Expr[UserDefinedAttributeHandler[PropName, DataType]],
  )(using
    quotes: Quotes,
    nameTpe: Type[PropName],
    rTpe: Type[DataType],
    attrTpe: AttrType,
  ) extends AttrMacrosDef[DataType] {
    import quotes.*
    import quotes.reflect.*

    override protected def withConstImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      constStr: String)(using MacrosPosition): Expr[MetatDataBinder] = {
      '{ (binding: NamespaceBinding, element: ReactiveElementBase) =>
        {
          val uri  = ${ namespace }.apply(binding)
          val hdlr = ${ handler }
          val data = hdlr.encode(${ Expr(constStr) })
          hdlr.withValue(uri, ${ Expr(prefix) }, ${ Expr(attrKey) }, data, element)
        }
      }
    }

    override protected def withExprImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      expr: Expr[T])(using MacrosPosition): Expr[MetatDataBinder] = {
      expr match {
        case v: Expr[DataType] @unchecked if TypeRepr.of[T] <:< TypeRepr.of[DataType] =>
          '{ (binding: NamespaceBinding, element: ReactiveElementBase) =>
            {
              val uri  = ${ namespace }.apply(binding)
              val hdlr = ${ handler }
              hdlr.withValue(uri, ${ Expr(prefix) }, ${ Expr(attrKey) }, ${ v }, element)
            }
          }

        case _ => MacorsMessage.expectationType[T, DataType]
      }
    }

    override def withExprFromSourceImpl[V: Type, CC <: Source[V]: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      expr: Expr[CC])(using MacrosPosition): Expr[MetatDataBinder] = {
      expr match {
        case sourceValue: Expr[Source[DataType]] @unchecked if TypeRepr.of[V] <:< TypeRepr.of[DataType] =>
          '{ (binding: NamespaceBinding, element: ReactiveElementBase) =>
            {
              val uri  = ${ namespace }.apply(binding)
              val hdlr = ${ handler }
              hdlr.withSourceValue(uri, ${ Expr(prefix) }, ${ Expr(attrKey) }, ${ sourceValue }, element)
            }
          }

        case _ => MacorsMessage.expectationType[CC, Source[DataType]]
      }
    }
  }
}
