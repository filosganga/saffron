package com.ovoenergy.arvo.ast

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.arvo.Varint
import com.ovoenergy.arvo.ast.Schema._

import scala.collection.immutable
trait Schema {
  type ValueType
}

object Schema {

  case object StringSchema extends Schema {
    override type ValueType = String
  }

  case object BooleanSchema extends Schema {
    override type ValueType = Boolean
  }

  case object BytesSchema extends Schema {
    override type ValueType = immutable.Iterable[Byte]
  }

  case object IntSchema extends Schema {
    override type ValueType = Int
  }

  case object FloatSchema extends Schema {
    override type ValueType = Float
  }

  case object DoubleSchema extends Schema {
    override type ValueType = Double
  }

  case object LongSchema extends Schema {
    override type ValueType = Long
  }

  case object NullSchema extends Schema {
    override type ValueType = Unit
  }

  case class FixedSchema(length: Long) extends Schema {
    override type ValueType = immutable.Iterable[Byte]
  }

  // TODO Quite complex to model
  case class RecordSchema(fieldsSchemas: immutable.Seq[(String, Schema)]) extends Schema {
    override type ValueType = immutable.Seq[(String, Avro[Schema])]
  }

  case class ArraySchema[T <: Schema](elementSchema: T) extends Schema {
    override type ValueType = immutable.Iterable[Avro[T]]
  }


}

sealed abstract class Avro[+T <: Schema](val schema: T) {
  def value: schema.ValueType
}

case class AvroString(value: String) extends Avro[StringSchema.type](StringSchema)

case class AvroInt(value: Int) extends Avro[IntSchema.type](IntSchema)

case class AvroLong(value: Long) extends Avro[LongSchema.type](LongSchema)

case class AvroFloat(value: Float) extends Avro[FloatSchema.type](FloatSchema)

case class AvroDouble(value: Double) extends Avro[DoubleSchema.type](DoubleSchema)

case class AvroBoolean(value: Boolean) extends Avro[BooleanSchema.type](BooleanSchema)

case class AvroBytes(value: immutable.Iterable[Byte]) extends Avro[BytesSchema.type](BytesSchema)

case class AvroFixed(value: immutable.Iterable[Byte]) extends Avro[FixedSchema](FixedSchema(value.size))

case class AvroArray[T <: Schema](elementSchema: T, value: immutable.Iterable[Avro[T]]) extends Avro[ArraySchema[T]](ArraySchema(elementSchema))

case class AvroRecord(value: immutable.Seq[(String, Avro[Schema])]) extends Avro[RecordSchema](RecordSchema(value.map{case (k,v) => k->v.schema}))

case object AvroNull extends Avro[NullSchema.type](NullSchema){
  override val value: Unit = Unit
}


object Avro {

  private val NullAsBinary = Array.empty[Byte]

  val Null = AvroNull

  def record(fields: (String, Avro[Schema])*): Avro[RecordSchema] = AvroRecord(immutable.Seq(fields:_*))

  def string(value: String): Avro[StringSchema.type] = AvroString(value)

  def fixed(value: Array[Byte]): Avro[FixedSchema] = fixed(value.toList)

  def fixed(value: immutable.Iterable[Byte]): Avro[FixedSchema] = AvroFixed(value)

  def bytes(value: Array[Byte]): Avro[BytesSchema.type] = bytes(value.toList)

  def bytes(value: immutable.Iterable[Byte]): Avro[BytesSchema.type] = AvroBytes(value)

  def int(value: Int): Avro[IntSchema.type] = AvroInt(value)

  def long(value: Long): Avro[LongSchema.type] = AvroLong(value)

  def float(value: Float): Avro[FloatSchema.type] = AvroFloat(value)

  def double(value: Double): Avro[DoubleSchema.type] = AvroDouble(value)

  def boolean(value: Boolean): Avro[BooleanSchema.type] = AvroBoolean(value)

  def array[T <: Schema](elementSchema: T, values: Avro[T]*): Avro[ArraySchema[T]] = AvroArray(elementSchema, immutable.Seq(values:_*))

  def binary(avro: Avro[Schema], buffer: ByteBuffer): Unit = avro match {
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
      Varint.writeSignedLong(value.size, buffer)
      buffer.put(value.toArray)

    case AvroFixed(value) =>
      buffer.put(value.toArray)

    case AvroRecord(values) =>
      // It is not tail recursive but it is fine for now since the object nesting deep level should not be an issue
      values.map(_._2).foreach(binary(_, buffer))

    case _ =>
      // TBC
      throw new UnsupportedOperationException("TBC")

  }

  def binary(avro: Avro[Schema]): Array[Byte] = {
    val buffer = ByteBuffer.allocate(1024)
    binary(avro, buffer)
    buffer.flip()
    val data = Array.ofDim[Byte](buffer.limit() - buffer.position())
    buffer.get(data)
    data
  }
}
