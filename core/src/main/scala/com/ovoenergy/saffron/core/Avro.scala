package com.ovoenergy.saffron.core

import com.ovoenergy.saffron.core.Schema._

import scala.collection.immutable

sealed trait Failure

case class ParsingFailure(description: String) extends Failure

trait Schema

trait NamedSchema extends Schema {
  def fullName: String
}

object Schema {

  case object StringSchema extends Schema

  case object BooleanSchema extends Schema

  case object BytesSchema extends Schema

  case object IntSchema extends Schema

  case object FloatSchema extends Schema

  case object DoubleSchema extends Schema

  case object LongSchema extends Schema

  case object NullSchema extends Schema

  case class UnionSchema(types: immutable.IndexedSeq[Schema]) extends Schema

  case class FixedSchema(length: Long) extends Schema

  case class RecordSchema(fullName: String, fieldsSchemas: immutable.Seq[(String, Schema)]) extends NamedSchema

  case class MapSchema(valueSchema: Schema) extends Schema

  case class ArraySchema(elementSchema: Schema) extends Schema

}

sealed abstract class Avro(val schema: Schema)

case class AvroString(value: String) extends Avro(StringSchema)

case class AvroInt(value: Int) extends Avro(IntSchema)

case class AvroFloat(value: Float) extends Avro(FloatSchema)

case class AvroDouble(value: Double) extends Avro(DoubleSchema)

case class AvroLong(value: Long) extends Avro(LongSchema)

case class AvroBoolean(value: Boolean) extends Avro(BooleanSchema)

case class AvroBytes(value: immutable.Seq[Byte]) extends Avro(BytesSchema)

// TODO It need some trick to make the compiler working for us and guarantee the consistency.
// It should be modelled similar to a coproduct in shapeless.
case class AvroUnion(types: immutable.IndexedSeq[Schema], typeIndex: Int, value: Avro) extends Avro(UnionSchema(types))

case class AvroFixed(length: Long, value: immutable.Seq[Byte]) extends Avro(FixedSchema(length))

// TODO Add type constraint: all the elements should have the same schema.
case class AvroArray(elementSchema: Schema, values: immutable.Seq[Avro]) extends Avro(ArraySchema(elementSchema))

// TODO Add type constraint: all the values should have the same schema.
case class AvroMap(valueSchema: Schema, values: Map[String, Avro]) extends Avro(MapSchema(valueSchema))

case class AvroRecord(fullName: String, fields: immutable.Seq[(String, Avro)])
    extends Avro(RecordSchema(fullName, fields.map { case (k, v) => k -> v.schema }))

case object AvroNull extends Avro(NullSchema)

object Avro {

  val Null = AvroNull

  def record(fullName: String, fields: (String, Avro)*): Avro = AvroRecord(fullName, immutable.Seq(fields: _*))

  def string(value: String): Avro = AvroString(value)

  def fixed(value: immutable.Seq[Byte]): Avro = fixed(value.length, value)

  def fixed(length: Long, value: immutable.Seq[Byte]): Avro = AvroFixed(length, value)

  def bytes(value: immutable.Seq[Byte]): Avro = AvroBytes(value)

  def int(value: Int): Avro = AvroInt(value)

  def float(value: Float): Avro = AvroFloat(value)

  def double(value: Double): Avro = AvroDouble(value)

  def long(value: Long): Avro = AvroLong(value)

  def boolean(value: Boolean): Avro = AvroBoolean(value)

  def array(elementSchema: Schema, values: Avro*): Avro = AvroArray(elementSchema, immutable.Seq(values: _*))

  def map(valueSchema: Schema, values: (String, Avro)*): Avro = AvroMap(valueSchema, values.toMap)

  // TODO How can we check at compile type that value.schema is included in types?
  def union(types: immutable.IndexedSeq[Schema], value: Avro): Avro = {
    val schemaIndex = types.indexOf(value.schema)
    require(schemaIndex != -1, "The value schema should be included in the Union types")
    AvroUnion(types, schemaIndex, value)
  }
}
