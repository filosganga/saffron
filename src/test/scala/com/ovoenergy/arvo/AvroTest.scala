package com.ovoenergy.arvo

import java.io.ByteArrayOutputStream

import com.ovoenergy.arvo.ast.Avro
import org.apache.avro.io.{Encoder, EncoderFactory}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

class AvroTest extends WordSpec with Matchers with PropertyChecks {

  "Avro" when {
    "serializing binary" should {
      "serialize null as empty Array" in {

        val expectedBytes = writeWithJavaAvro(_.writeNull())
        Avro.binary(Avro.Null) should contain theSameElementsInOrderAs expectedBytes
      }

      "serialize boolean as single byte" in forAll { boolean: Boolean =>

        val expectedBytes = writeWithJavaAvro(_.writeBoolean(boolean))
        Avro.binary(Avro.boolean(boolean)).deep shouldBe expectedBytes.deep
      }

      "serialize Int as zig-zag varint" in forAll { int: Int =>

        val expectedBytes = writeWithJavaAvro(_.writeInt(int))
        Avro.binary(Avro.int(int)).deep shouldBe expectedBytes.deep
      }

      "serialize Long as zig-zag varint" in forAll { long: Long =>

        val expectedBytes = writeWithJavaAvro(_.writeLong(long))
        Avro.binary(Avro.long(long)).deep shouldBe expectedBytes.deep
      }

      "serialize String length as zig-zag varint" in forAll { string: String =>

        val expectedBytes = writeWithJavaAvro(_.writeString(string))
        Avro.binary(Avro.string(string)).deep shouldBe expectedBytes.deep
      }

      "serialize bytes as zig-zag varint length followed by the bytes" in forAll { bytes: Array[Byte] =>

        val expectedBytes = writeWithJavaAvro(_.writeBytes(bytes))
        Avro.binary(Avro.bytes(bytes)).deep shouldBe expectedBytes.deep
      }

      "serialize fixed as fixed length byte array" in forAll { bytes: Array[Byte] =>

        val expectedBytes = writeWithJavaAvro(_.writeFixed(bytes))
        Avro.binary(Avro.fixed(bytes)).deep shouldBe expectedBytes.deep
      }

    }
  }

  def writeWithJavaAvro(f: Encoder => Unit): Array[Byte] = {
    val bout = new ByteArrayOutputStream
    val encoder = EncoderFactory.get().binaryEncoder(bout, null)

    f(encoder)
    encoder.flush()
    bout.flush()
    bout.toByteArray
  }

}
