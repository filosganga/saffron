package com.ovoenergy.arvo.ast

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.arvo.Varint
import com.ovoenergy.arvo.ast.Schema._

import scala.collection.immutable
trait Schema

object Schema {

  case object StringSchema extends Schema

  case object BooleanSchema extends Schema

  case object BytesSchema extends Schema

  case object IntSchema extends Schema

  case object LongSchema extends Schema

  case object NullSchema extends Schema

  case class FixedSchema(length: Long) extends Schema

  case class RecordSchema(fieldsSchemas: immutable.Seq[(String, Schema)]) extends Schema

  case class ArraySchema(elementSchema: Schema) extends Schema


}

sealed abstract class Avro(val schema: Schema)

case class AvroString(value: String) extends Avro(StringSchema)

case class AvroInt(value: Int) extends Avro(IntSchema)

case class AvroLong(value: Long) extends Avro(LongSchema)

case class AvroBoolean(value: Boolean) extends Avro(BooleanSchema)

case class AvroBytes(value: Array[Byte]) extends Avro(BytesSchema)

case class AvroFixed(value: Array[Byte]) extends Avro(FixedSchema(value.length))

case class AvroArray(elementSchema: Schema, values: immutable.Iterable[Avro]) extends Avro(ArraySchema(elementSchema))

case class AvroRecord(fields: immutable.Seq[(String, Avro)]) extends Avro(RecordSchema(fields.map{case (k,v) => k->v.schema}))

case object AvroNull extends Avro(NullSchema)


object Avro {

  private val NullAsBinary = Array.empty[Byte]

  val Null = AvroNull

  def record(fields: (String,Avro)*): Avro = AvroRecord(immutable.Seq(fields:_*))

  def string(value: String): Avro = AvroString(value)

  def fixed(value: Array[Byte]): Avro = AvroFixed(value)

  def bytes(value: Array[Byte]): Avro = AvroBytes(value)

  def int(value: Int): Avro = AvroInt(value)

  def long(value: Long): Avro = AvroLong(value)

  def boolean(value: Boolean): Avro = AvroBoolean(value)

  def array(elementSchema: Schema, values: Avro*): Avro = AvroArray(elementSchema, immutable.Seq(values:_*))

  def binary(avro: Avro, buffer: ByteBuffer): Unit = avro match {
    case AvroNull =>

    case AvroInt(value) =>
      Varint.writeSignedInt(value, buffer)

    case AvroLong(value) =>
      Varint.writeSignedLong(value, buffer)

    case AvroBoolean(value) =>
      if (value)
        buffer.put(1: Byte)
      else
        buffer.put(0: Byte)

    case AvroString(value) =>
      Varint.writeSignedLong(value.length, buffer)
      buffer.put(value.getBytes(UTF_8))

    case AvroBytes(value) =>
      Varint.writeSignedLong(value.length, buffer)
      buffer.put(value)

    case AvroFixed(value) =>
      buffer.put(value)

    case AvroRecord(values) =>
      // It is not tail recursive but it is fine for now since the object nesting deep level should not be an issue
      values.map(_._2).foreach(binary(_, buffer))

    case AvroArray(_, _) =>
      // TBC
      throw new UnsupportedOperationException("TBC")

  }

  def binary(avro: Avro): Array[Byte] = {
    val buffer = ByteBuffer.allocate(1024)
    binary(avro, buffer)
    buffer.flip()
    val data = Array.ofDim[Byte](buffer.limit() - buffer.position())
    buffer.get(data)
    data
  }
}