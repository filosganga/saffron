package com.ovoenergy.arvo

import java.nio.charset.StandardCharsets

import com.ovoenergy.arvo.ast.Schema._
import com.ovoenergy.arvo.ast._

import scala.annotation.implicitNotFound

@implicitNotFound("Not implicit defined for SchemaResolver...")
trait SchemaResolver[From <: Schema, To <: Schema] {

  def resolve(from: Avro[From], to: To): Avro[To]
}

object SchemaResolver {

  def resolve[From <: Schema, To <: Schema](fromAvro: Avro[From], toSchema: To)(implicit resolver: SchemaResolver[From, To]): Avro[To] = {
    resolver.resolve(fromAvro, toSchema)
  }

}

object SchemaResolverInstances {

  implicit def selfResolver[T <: Schema]: SchemaResolver[T,T] = new SchemaResolver[T,T] {
    override def resolve(from: Avro[T], to: T): Avro[T] = from
  }

  implicit object IntToLongResolver extends SchemaResolver[IntSchema.type, LongSchema.type] {
    override def resolve(from: Avro[IntSchema.type], to: LongSchema.type): Avro[LongSchema.type] = AvroLong(from.value.toLong)
  }

  implicit object IntToFloatResolver extends SchemaResolver[IntSchema.type, FloatSchema.type] {
    override def resolve(from: Avro[IntSchema.type], to: FloatSchema.type): Avro[FloatSchema.type] = AvroFloat(from.value.toFloat)
  }

  implicit object IntToDoubleResolver extends SchemaResolver[IntSchema.type, DoubleSchema.type] {
    override def resolve(from: Avro[IntSchema.type], to: DoubleSchema.type): Avro[DoubleSchema.type] = AvroDouble(from.value.toDouble)
  }

  implicit object LongToFloatResolver extends SchemaResolver[LongSchema.type, FloatSchema.type] {
    override def resolve(from: Avro[LongSchema.type], to: FloatSchema.type): Avro[FloatSchema.type] = AvroFloat(from.value.toFloat)
  }

  implicit object LongToDoubleResolver extends SchemaResolver[LongSchema.type, DoubleSchema.type] {
    override def resolve(from: Avro[LongSchema.type], to: DoubleSchema.type): Avro[DoubleSchema.type] = AvroDouble(from.value.toDouble)
  }

  implicit object FloatToDoubleResolver extends SchemaResolver[FloatSchema.type, DoubleSchema.type] {
    override def resolve(from: Avro[FloatSchema.type], to: DoubleSchema.type): Avro[DoubleSchema.type] = AvroDouble(from.value.toDouble)
  }

  implicit object StringToBytesResolver extends SchemaResolver[StringSchema.type, BytesSchema.type] {
    override def resolve(from: Avro[StringSchema.type], to: BytesSchema.type): Avro[BytesSchema.type] = AvroBytes(from.value.getBytes(StandardCharsets.UTF_8).toList)
  }

  implicit object BytesToStringToResolver extends SchemaResolver[BytesSchema.type, StringSchema.type] {
    override def resolve(from: Avro[BytesSchema.type], to: StringSchema.type): Avro[StringSchema.type] = AvroString(new String(from.value.toArray, StandardCharsets.UTF_8))
  }

  implicit def arrayResolver[F <: Schema, T <: Schema](implicit elementSchemaResolver: SchemaResolver[F,T]): SchemaResolver[ArraySchema[F], ArraySchema[T]] = new SchemaResolver[ArraySchema[F], ArraySchema[T]] {
    override def resolve(from: Avro[ArraySchema[F]], to: ArraySchema[T]): Avro[ArraySchema[T]] = {
      AvroArray(to.elementSchema, from.value.map(elementSchemaResolver.resolve(_, to.elementSchema)))
    }
  }
}