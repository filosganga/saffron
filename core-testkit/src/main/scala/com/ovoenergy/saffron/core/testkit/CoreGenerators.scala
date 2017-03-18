package com.ovoenergy.saffron.core.testkit

import com.ovoenergy.saffron.core._
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._

trait CoreGenerators {

  import Gen._
  import Arbitrary._
  import Schema._

  implicit lazy val arbAvroNull: Arbitrary[AvroNull.type] = Arbitrary(const(AvroNull))

  implicit lazy val arbAvroInt: Arbitrary[AvroInt] = Arbitrary(for {
    value <- arbitrary[Int]
  } yield AvroInt(value))

  implicit lazy val arbAvroFloat: Arbitrary[AvroFloat] = Arbitrary(for {
    value <- arbitrary[Float]
  } yield AvroFloat(value))

  implicit lazy val arbAvroDouble: Arbitrary[AvroDouble] = Arbitrary(for {
    value <- arbitrary[Double]
  } yield AvroDouble(value))

  implicit lazy val arbAvroLong: Arbitrary[AvroLong] = Arbitrary(for {
    value <- arbitrary[Long]
  } yield AvroLong(value))

  implicit lazy val arbAvroString: Arbitrary[AvroString] = Arbitrary(for {
    value <- arbitrary[String]
  } yield AvroString(value))

  implicit lazy val arbAvroArray: Arbitrary[AvroArray] = Arbitrary(for {
    xs <- listOf(arbitrary[AvroInt])
  } yield AvroArray(IntSchema, xs))

  implicit lazy val arbAvroRecord: Arbitrary[AvroRecord] = Arbitrary(for {
    name <- arbitrary[String]
    xs <- listOf(zip(arbitrary[String], lzy(arbitrary[Avro])))
  } yield AvroRecord(name, xs))

  implicit lazy val arbAvro: Arbitrary[Avro] = Arbitrary(
    oneOf(
      arbitrary[AvroNull.type],
      arbitrary[AvroInt],
      arbitrary[AvroFloat],
      arbitrary[AvroDouble],
      arbitrary[AvroLong],
      arbitrary[AvroString],
      arbitrary[AvroArray]
    )
  )

}

object CoreGenerators extends CoreGenerators
