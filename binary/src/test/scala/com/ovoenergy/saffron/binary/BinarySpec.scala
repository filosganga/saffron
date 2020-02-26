package com.ovoenergy.saffron.binary

import java.io.ByteArrayOutputStream

import com.ovoenergy.saffron.core
import com.ovoenergy.saffron.core.Schema._
import com.ovoenergy.saffron.core.{CoreGenerators, _}
import org.apache.avro.io.{Encoder, EncoderFactory}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import scodec.bits.BitVector

class BinarySpec extends WordSpec with Matchers with PropertyChecks with CoreGenerators {

  def writeWithJavaAvro(f: Encoder => Unit): BitVector = {
    val bout = new ByteArrayOutputStream
    val encoder = EncoderFactory.get().binaryEncoder(bout, null)

    f(encoder)
    encoder.flush()
    bout.flush()
    BitVector.view(bout.toByteArray)
  }

  "Binary" should {
    "serialize null" in {
      val expected = writeWithJavaAvro(_.writeNull())

      Binary.codecForSchema(NullSchema).encode(AvroNull).toEither shouldBe Right(expected)
    }

    "deserialize null" in {
      val bits = writeWithJavaAvro(_.writeNull())
      Binary.codecForSchema(NullSchema).decodeValue(bits).toEither shouldBe Right(AvroNull)
    }

    "serialize int" in forAll() { x: Int =>
      val expected = writeWithJavaAvro(_.writeInt(x))

      Binary.codecForSchema(IntSchema).encode(AvroInt(x)).toEither shouldBe Right(expected)
    }

    "deserialize int" in forAll() { x: Int =>
      val bits = writeWithJavaAvro(_.writeInt(x))

      Binary.codecForSchema(IntSchema).decodeValue(bits).toEither shouldBe Right(AvroInt(x))
    }

    "serialize long" in forAll() { x: Long =>
      val expected = writeWithJavaAvro(_.writeLong(x))

      Binary.codecForSchema(LongSchema).encode(AvroLong(x)).toEither shouldBe Right(expected)
    }

    "deserialize long" in forAll() { x: Long =>
      val bits = writeWithJavaAvro(_.writeLong(x))

      Binary.codecForSchema(LongSchema).decodeValue(bits).toEither shouldBe Right(AvroLong(x))
    }

    "serialize float" in forAll() { x: Float =>
      val expected = writeWithJavaAvro(_.writeFloat(x))

      Binary.codecForSchema(FloatSchema).encode(AvroFloat(x)).toEither shouldBe Right(expected)
    }

    "deserialize float" in forAll() { x: Float =>
      val bits = writeWithJavaAvro(_.writeFloat(x))

      Binary.codecForSchema(FloatSchema).decodeValue(bits).toEither shouldBe Right(AvroFloat(x))
    }

    "serialize double" in forAll() { x: Double =>
      val expected = writeWithJavaAvro(_.writeDouble(x))

      Binary.codecForSchema(DoubleSchema).encode(AvroDouble(x)).toEither shouldBe Right(expected)
    }

    "deserialize double" in forAll() { x: Double =>
      val bits = writeWithJavaAvro(_.writeDouble(x))

      Binary.codecForSchema(DoubleSchema).decodeValue(bits).toEither shouldBe Right(AvroDouble(x))
    }

    "serialize string" in forAll() { x: String =>
      val expected = writeWithJavaAvro(_.writeString(x))
      Binary.codecForSchema(StringSchema).encode(AvroString(x)).toEither shouldBe Right(expected)
    }

    "deserialize string" in forAll() { x: String =>
      val bits = writeWithJavaAvro(_.writeString(x))

      Binary.codecForSchema(StringSchema).decodeValue(bits).toEither shouldBe Right(AvroString(x))
    }

    "serialize fixed" in forAll() { xs: Array[Byte] =>
      val expected = writeWithJavaAvro(_.writeFixed(xs))
      Binary.codecForSchema(FixedSchema(xs.length)).encode(AvroFixed(xs.toList, xs.length)).toEither shouldBe Right(expected)
    }

    "deserialize fixed" in forAll() { xs: Array[Byte] =>
      val bits = writeWithJavaAvro(_.writeFixed(xs))

      Binary.codecForSchema(FixedSchema(xs.length)).decodeValue(bits).toEither shouldBe Right(AvroFixed(xs.toList, xs.length))
    }

  }

}
