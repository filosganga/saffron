package com.ovoenergy.saffron.binary

import java.io.ByteArrayOutputStream
import java.util.{Iterator => JIterator, Map => JMap}

import com.ovoenergy.saffron.core.Schema._
import com.ovoenergy.saffron.core.{CoreGenerators, _}
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.io.{Encoder, EncoderFactory}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

import scala.collection.JavaConverters._

class BinarySpec extends WordSpec with Matchers with PropertyChecks with CoreGenerators {
  import Binary._

  "Avro" when {
    "serializing binary" should {
      "serialize null as empty Array" in {

        val expectedBytes = writeWithJavaAvro(_.writeNull())
        encode(Avro.Null) should contain theSameElementsInOrderAs expectedBytes
      }

      "serialize boolean as single byte" in forAll { boolean: Boolean =>
        val expectedBytes = writeWithJavaAvro(_.writeBoolean(boolean))
        encode(Avro.boolean(boolean)).deep shouldBe expectedBytes.deep
      }

      "serialize Int as zig-zag varint" in forAll { int: Int =>
        val expectedBytes = writeWithJavaAvro(_.writeInt(int))
        encode(Avro.int(int)).deep shouldBe expectedBytes.deep
      }

      "serialize Float" in forAll { float: Float =>
        val expectedBytes = writeWithJavaAvro(_.writeFloat(float))
        encode(Avro.float(float)).deep shouldBe expectedBytes.deep
      }

      "serialize Double" in forAll { double: Double =>
        val expectedBytes = writeWithJavaAvro(_.writeDouble(double))
        encode(Avro.double(double)).deep shouldBe expectedBytes.deep
      }

      "serialize Long as zig-zag varint" in forAll { long: Long =>
        val expectedBytes = writeWithJavaAvro(_.writeLong(long))
        encode(Avro.long(long)).deep shouldBe expectedBytes.deep
      }

      "serialize String length as zig-zag varint followed by UTF-8 bytes" in forAll { string: String =>
        val expectedBytes = writeWithJavaAvro(_.writeString(string))
        encode(Avro.string(string)).deep shouldBe expectedBytes.deep
      }

      "serialize bytes as zig-zag varint length followed by the bytes" in forAll { bytes: Array[Byte] =>
        val expectedBytes = writeWithJavaAvro(_.writeBytes(bytes))
        encode(Avro.bytes(bytes.toList)).deep shouldBe expectedBytes.deep
      }

      "serialize fixed as fixed length byte array" in forAll { bytes: Array[Byte] =>
        val expectedBytes = writeWithJavaAvro(_.writeFixed(bytes))
        encode(Avro.fixed(bytes.toList)).deep shouldBe expectedBytes.deep
      }

      "be able serialize an array " in forAll { strings: Seq[String] =>
        val expectedBytes = writeWithJavaAvro { e =>
          e.writeArrayStart()
          e.setItemCount(strings.length)
          strings.foreach(e.writeString)
          e.writeArrayEnd()
        }
        encode(Avro.array(StringSchema, strings.map(AvroString.apply): _*)).deep shouldBe expectedBytes.deep
      }

    }

    "parsing binary" should {

      "be able to parse a null" in {
        val bytes = writeWithJavaAvro(_.writeNull())
        decode(NullSchema, bytes) shouldBe Right(AvroNull)
      }

      "be able to parse an Int" in forAll() { int: Int =>
        val bytes = writeWithJavaAvro(_.writeInt(int))
        decode(IntSchema, bytes) shouldBe Right(AvroInt(int))
      }

      "be able to parse a float" in forAll() { value: Float =>
        val bytes = writeWithJavaAvro(_.writeFloat(value))
        decode(FloatSchema, bytes) shouldBe Right(AvroFloat(value))
      }

      "be able to parse a double" in forAll() { value: Double =>
        val bytes = writeWithJavaAvro(_.writeDouble(value))
        decode(DoubleSchema, bytes) shouldBe Right(AvroDouble(value))
      }

      "be able to parse a long" in forAll() { value: Long =>
        val bytes = writeWithJavaAvro(_.writeLong(value))
        decode(LongSchema, bytes) shouldBe Right(AvroLong(value))
      }

      "be able to parse a string" in forAll() { value: String =>
        val bytes = writeWithJavaAvro(_.writeString(value))
        decode(StringSchema, bytes) shouldBe Right(AvroString(value))
      }

      "be able to parse a fixed number of bytes" in forAll() { value: List[Byte] =>
        val bytes = writeWithJavaAvro(_.writeFixed(value.toArray))
        decode(FixedSchema(value.length), bytes) shouldBe Right(AvroFixed(value, value.size))
      }

      "be able to parse an array of bytes" in forAll() { value: Array[Byte] =>
        val bytes = writeWithJavaAvro(_.writeBytes(value))
        decode(BytesSchema, bytes) shouldBe Right(AvroBytes(value.toList))
      }

      "be able to parse a boolean" in forAll() { value: Boolean =>
        val bytes = writeWithJavaAvro(_.writeBoolean(value))
        decode(BooleanSchema, bytes) shouldBe Right(AvroBoolean(value))
      }

      "be able to parse an array" in forAll() { strings: List[String] =>
        val bytes = writeWithJavaAvro { e =>
          e.writeArrayStart()
          e.setItemCount(strings.size)
          strings.foreach(e.writeString)
          e.writeArrayEnd()
        }

        decode(ArraySchema(StringSchema), bytes) shouldBe Right(Avro.array(StringSchema, strings.map(AvroString.apply):_*))
      }

      "be able to parse a map" in forAll() { map: Map[String, Int] =>
        val bytes = writeWithJavaAvro { e =>
          val writer =
            new GenericDatumWriter[JMap[String, Int]](SchemaBuilder.map().values(SchemaBuilder.builder().intType()))
          writer.write(map.asJava, e)
        }
        decode(MapSchema(IntSchema), bytes) shouldBe Right(Avro.map(IntSchema, map.mapValues(AvroInt.apply).toList:_*))
      }

      "be able to parse a union type" in forAll() { string: String =>
        val schema = UnionSchema(Vector(IntSchema, StringSchema))
        val bytes = writeWithJavaAvro { e =>
          e.writeIndex(1)
          e.writeString(string)
        }
        decode(schema, bytes) shouldBe Right(Avro.union(AvroString(string), IntSchema, StringSchema))
      }

      "be able to parse a record" in forAll() { (name: String, fields: Map[String, AvroString]) =>
        val bytes = writeWithJavaAvro { e =>
          fields.foreach {
            case (_, value) =>
              e.writeString(value.value)
          }
        }
        decode(RecordSchema(name, fields.map(x => x._1 -> x._2.schema)), bytes) shouldBe Right(
          AvroRecord(name, fields.mapValues(_.asInstanceOf[Avro[Schema]]))
        )
      }
    }

    "round-trip" should {
      "handle AvroInt" in forAll() { avro: AvroInt =>
        decode(avro.schema, encode(avro)) shouldBe Right(avro)
      }

      "handle AvroFloat" in forAll() { avro: AvroFloat =>
        decode(avro.schema, encode(avro)) shouldBe Right(avro)
      }

      "handle AvroDouble" in forAll() { avro: AvroDouble =>
        decode(avro.schema, encode(avro)) shouldBe Right(avro)
      }

      "handle AvroLong" in forAll() { avro: AvroLong =>
        decode(avro.schema, encode(avro)) shouldBe Right(avro)
      }

      "handle any Avro" in forAll() { avro: Avro[Schema] =>
        decode(avro.schema, encode(avro)) shouldBe Right(avro)
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
