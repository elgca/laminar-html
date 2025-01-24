package scala.xml
package binders

import scala.quoted.*
import scala.xml.MacorsMessage.{????, AttrType}
import scala.xml.udf.UserDefinedAttributeHandler
import scala.xml.{MacrosPosition, NamespaceBinding}

object UdfHandlers {

  def unapply[T](
    tuple: (Option[String], String, Type[T]),
  )(using quotes: Quotes): Option[(String, AttrMacrosDef[?], Seq[(String, String)])] = {
    val (prefix: Option[String], attrKey: String, tpe: Type[T]) = tuple
    import quotes.*
    import quotes.reflect.*

    val name = prefix.map(_ + ":").getOrElse("") + attrKey

    def summonMacrosDefFrom(base: TypeRepr, keyType: TypeRepr, valueType: TypeRepr): Option[UdfHandlersMacros[?, ?]] = {
      val handleType = AppliedType(base, List(keyType, valueType)).asType
      Implicits.search(TypeRepr.of(using handleType)) match {
        case iss: ImplicitSearchSuccess =>
          handleType match {
            case '[UserDefinedAttributeHandler[a, b]] =>
              given attrType: AttrType =
                AttrType(s"${iss.tree.asExpr.show}:<${name}>]")
              val handlerExpr          = iss.tree.asExpr.asInstanceOf[Expr[UserDefinedAttributeHandler[a, b]]]
              Some(UdfHandlersMacros[a, b](handlerExpr))
          }
        case isf: ImplicitSearchFailure => None
      }
    }

    for
      hType  <- TypeRepr.of[UserDefinedAttributeHandler[?, ?]] match {
                  case AppliedType(tpe, _) => Some(tpe)
                  case _                   => None
                }
      keyType = ConstantType(StringConstant(name))
      macros <- summonMacrosDefFrom(hType, keyType, TypeRepr.of(using tpe))
                  .orElse {
                    summonMacrosDefFrom(hType, keyType, TypeBounds.empty)
                  }
    yield (attrKey, macros, Seq(macros.supportedTypesMessage))
  }

  class UdfHandlersMacros[PropName, DataType](
    handler: Expr[UserDefinedAttributeHandler[PropName, DataType]],
  )(using
    quotes: Quotes,
    nameTpe: Type[PropName],
    rTpe: Type[DataType],
    val attrTpe: AttrType,
  ) extends AttrMacrosDef[DataType] {
    import quotes.*
    import quotes.reflect.*

    override def supportConst: Boolean = false

    override protected def withConstImpl[T: Type](
      namespace: Expr[NamespaceBinding => Option[String]],
      prefix: Option[String],
      attrKey: String,
      constStr: String)(using MacrosPosition): Expr[MetatDataBinder] = {
      ????
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
