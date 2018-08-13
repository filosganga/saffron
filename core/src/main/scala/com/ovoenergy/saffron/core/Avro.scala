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

  case class RecordSchema(fullName: String, fieldsSchemas: Map[String, Schema]) extends NamedSchema

  case class MapSchema[T <: Schema](valueSchema: T) extends Schema

  case class ArraySchema[T <: Schema](elementSchema: T) extends Schema

  case class EnumSchema(fullName: String, symbols: immutable.IndexedSeq[String]) extends NamedSchema

}

sealed abstract class Avro[T <: Schema] {
  def schema: T
}

case object AvroNull extends Avro[NullSchema.type] {
  override val schema: NullSchema.type = NullSchema
}

case class AvroInt(value: Int) extends Avro[IntSchema.type] {
  override val schema: IntSchema.type = IntSchema
}

case class AvroFloat(value: Float) extends Avro[FloatSchema.type] {
  override val schema: FloatSchema.type = FloatSchema
}

case class AvroDouble(value: Double) extends Avro[DoubleSchema.type] {
  override val schema: DoubleSchema.type = DoubleSchema
}

case class AvroLong(value: Long) extends Avro[LongSchema.type] {
  override val schema: LongSchema.type = LongSchema
}

case class AvroBoolean(value: Boolean) extends Avro[BooleanSchema.type] {
  override val schema: BooleanSchema.type = BooleanSchema
}

case class AvroBytes(value: immutable.Seq[Byte]) extends Avro[BytesSchema.type] {
  override val schema: BytesSchema.type = BytesSchema
}

case class AvroFixed(value: immutable.Seq[Byte], length: Long) extends Avro[FixedSchema] {
  override def schema: FixedSchema = FixedSchema(length)
}

case class AvroString(value: String) extends Avro[StringSchema.type] {
  override val schema: StringSchema.type = StringSchema
}


// TODO It needs some trick to make the compiler working for us and guarantee the consistency.
case class AvroUnion[T <: Schema](value: Avro[T], typeIndex: Int, types: immutable.IndexedSeq[Schema]) extends Avro[UnionSchema] {
  override val schema: UnionSchema = UnionSchema(types)
}

case class AvroArray[T <: Schema](values: immutable.Seq[Avro[T]], elementSchema: T) extends Avro[ArraySchema[T]] {
  override val schema: ArraySchema[T] = ArraySchema(elementSchema)
}

case class AvroMap[T <: Schema](values: Map[String, Avro[T]], valueSchema: T) extends Avro[MapSchema[T]] {
  override val schema: MapSchema[T] = MapSchema(valueSchema)
}

case class AvroRecord(name: String, fields: Map[String, Avro[Schema]]) extends Avro[RecordSchema] {
  override val schema: RecordSchema = RecordSchema(name, fields.mapValues(_.schema))
}

// TODO make sure symbolIndex < schema.types.length at compile time
case class AvroEnum(name: String, symbolIndex: Int, symbols: immutable.IndexedSeq[String]) extends Avro[EnumSchema] {
  override val schema: EnumSchema = EnumSchema(name, symbols)
  val symbol: String = schema.symbols(symbolIndex)
}

object Avro {

  val Null: Avro[NullSchema.type] = AvroNull

  def int(value: Int): Avro[IntSchema.type] =
    AvroInt(value)

  def float(value: Float): Avro[FloatSchema.type] =
    AvroFloat(value)

  def double(value: Double): Avro[DoubleSchema.type] =
    AvroDouble(value)

  def long(value: Long): Avro[LongSchema.type] =
    AvroLong(value)

  def boolean(value: Boolean): Avro[BooleanSchema.type] =
    AvroBoolean(value)

  def fixed(value: immutable.Seq[Byte]): Avro[FixedSchema] =
    fixed(value, value.length.toLong)

  def fixed(value: immutable.Seq[Byte], length: Long): Avro[FixedSchema] =
    AvroFixed(value, length)

  def bytes(value: immutable.Seq[Byte]): Avro[BytesSchema.type] = AvroBytes(value)

  def array[T <: Schema](elementSchema: T)(values: Avro[T]*): Avro[ArraySchema[T]] =
    AvroArray[T](values.toVector, elementSchema)

  def map[T <: Schema](valueSchema: T)(values: (String, Avro[T])*): Avro[MapSchema[T]] =
    AvroMap[T](values.toMap, valueSchema)

  // TODO How can we check at compile type that value.schema is included in types?
  def union[T <: Schema](value: Avro[T], types: Schema*): Avro[UnionSchema] = {
    val schemaIndex = types.indexOf(value.schema)
    require(schemaIndex != -1, "The value schema should be included in the Union types")
    AvroUnion[T](value, schemaIndex, types.toVector)
  }

  def record(fullName: String, fields: (String, Avro[Schema])*): Avro[RecordSchema] =
    AvroRecord(fullName, fields.toMap)

  def string(value: String): Avro[StringSchema.type] = AvroString(value)

  def enum(fullName: String, symbol: String, symbols: String*): Avro[EnumSchema] =
    AvroEnum(fullName, symbols.indexOf(symbol), symbols.toVector)


}
