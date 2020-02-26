package com.ovoenergy.saffron.core

import com.ovoenergy.saffron.core.Schema._
import org.scalacheck.{Arbitrary, Gen}

trait CoreGenerators {

  import Arbitrary._
  import Gen._

  implicit lazy val arbNullSchema: Arbitrary[NullSchema.type] =
    Arbitrary(const(NullSchema))

  implicit lazy val arbIntSchema: Arbitrary[IntSchema.type] =
    Arbitrary(const(IntSchema))

  implicit lazy val arbStringSchema: Arbitrary[StringSchema.type] =
    Arbitrary(const(StringSchema))

  implicit lazy val arbSchema: Arbitrary[Schema] = Arbitrary(
    Gen.oneOf(arbNullSchema.arbitrary, arbIntSchema.arbitrary, arbStringSchema.arbitrary)
  )

  implicit lazy val arbAvroNull: Arbitrary[AvroNull.type] = Arbitrary(const(AvroNull))

  implicit lazy val arbAvroInt: Arbitrary[AvroInt] = Arbitrary{
    for {
      n <- arbitrary[Int]
    } yield AvroInt(n)
  }

  implicit lazy val arbAvroLong: Arbitrary[AvroLong] = Arbitrary{
    for {
      n <- arbitrary[Long]
    } yield AvroLong(n)
  }

  implicit lazy val arbAvroFloat: Arbitrary[AvroFloat] = Arbitrary{
    for {
      n <- arbitrary[Float]
    } yield AvroFloat(n)
  }

  implicit lazy val arbAvroDouble: Arbitrary[AvroDouble] = Arbitrary{
    for {
      n <- arbitrary[Double]
    } yield AvroDouble(n)
  }

  implicit lazy val arbAvroString: Arbitrary[AvroString] = Arbitrary{
    for {
      str <- arbitrary[String]
    } yield AvroString(str)
  }

  implicit def arbAvro[T <: Schema](implicit arbT: Arbitrary[T]): Arbitrary[Avro[T]] = Arbitrary(
    for {
      schema <- arbT.arbitrary
      avro <- (schema match {
        case NullSchema => arbAvroNull
        case IntSchema => arbAvroInt
        case StringSchema => arbAvroString
      }).arbitrary
    } yield avro.asInstanceOf[Avro[T]]
  )
}

object CoreGenerators extends CoreGenerators
