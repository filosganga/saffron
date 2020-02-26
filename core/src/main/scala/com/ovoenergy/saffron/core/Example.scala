package com.ovoenergy.saffron.core
import scala.collection.immutable.ListSet

object Example extends App {

  sealed trait Schema[A <: Value]

  case object IntSchema extends Schema[IntValue]

  case object StringSchema extends Schema[StringValue]

  case class ArraySchema[A <: Value, B <: Schema[A]](itemsSchema: B) extends Schema[ArrayValue[A]]

  case class UnionSchema(schemas: ListSet[Set[Schema[_]]]) extends Schema[UnionType]

  sealed trait Value
  case class IntValue(value: Int) extends Value
  case class StringValue(value: String) extends Value
  case class ArrayValue[A <: Value](items: List[A]) extends Value


  trait Encoder[-A] {
    def encode(a: A): String
  }

  implicit object IntEncoder extends Encoder[IntValue] {
    def encode(a: IntValue): String = a.value.toString
  }

  implicit object StringEncoder extends Encoder[StringValue] {
    def encode(a: StringValue): String = s"${a.value.length};${a.value}"
  }

  implicit def arrayEncoder[A <: Value](implicit itemEncoder: Encoder[A]): Encoder[ArrayValue[A]] =
    new Encoder[ArrayValue[A]] {
      def encode(a: ArrayValue[A]): String = {
        val encodedItems = a.items.map(itemEncoder.encode).mkString("|")
        s"${encodedItems.length};$encodedItems"
      }
    }

  trait Decoder[+A] {
    def decode(encoded: String): A
  }

  implicit object IntDecoder extends Decoder[IntValue] {
    override def decode(encoded: String): IntValue = IntValue(encoded.toInt)
  }

  implicit object StringDecoder extends Decoder[StringValue] {
    override def decode(encoded: String): StringValue = {
      val semicolumnIndex = encoded.indexOf(";")
      val length = encoded.substring(0, semicolumnIndex).toInt
      StringValue(encoded.substring(semicolumnIndex + 1, semicolumnIndex + 1 + length))
    }
  }

  implicit def arrayDecoder[A <: Value](implicit aDecoder: Decoder[A]): Decoder[ArrayValue[A]] =
    new Decoder[ArrayValue[A]] {
      override def decode(encoded: String): ArrayValue[A] = {
        val semicolumnIndex = encoded.indexOf(";")
        val length = encoded.substring(0, semicolumnIndex).toInt
        val items = encoded.substring(semicolumnIndex + 1, length + 1 + semicolumnIndex).split('|').map(aDecoder.decode)
        ArrayValue(items.toList)
      }
    }

  def encode[A <: Value](a: A, s: Schema[A])(implicit encoder: Encoder[A]): String =
    encoder.encode(a)

  def decode[A <: Value](encoded: String, s: Schema[A])(implicit decoder: Decoder[A]): A =
    decoder.decode(encoded)

  println(encode(IntValue(8), IntSchema))
  println(encode(StringValue("Hello world"), StringSchema))
  println(encode(ArrayValue(List(IntValue(1), IntValue(2))), ArraySchema[IntValue, IntSchema.type](IntSchema)))

  val invalidValue: ArrayValue[Value] = ArrayValue(List(IntValue(1), StringValue("This is a string")))

  println(decode(encode(IntValue(8), IntSchema), IntSchema))
  println(decode(encode(StringValue("Hello world"), StringSchema), StringSchema))
  println(
    decode(
      encode(ArrayValue(List(IntValue(1), IntValue(2))), ArraySchema[IntValue, IntSchema.type](IntSchema)),
      ArraySchema[IntValue, IntSchema.type](IntSchema)
    )
  )

}
