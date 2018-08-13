package com.ovoenergy.saffron.binary

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.saffron.core.Schema._
import com.ovoenergy.saffron.core._

import scala.util.{Failure, Try}
import scala.collection.immutable
import scala.util.control.NonFatal

import cats.syntax.all._
import cats.instances.all._

object Binary {

  def encode(avro: Avro[_], buffer: ByteBuffer): Unit = avro match {
    case AvroNull =>
    case AvroInt(value) =>
      Varint.writeSignedInt(value, buffer)

    case AvroFloat(value) =>
      buffer.order(ByteOrder.LITTLE_ENDIAN)
      buffer.putFloat(value)

    case AvroDouble(value) =>
      buffer.order(ByteOrder.LITTLE_ENDIAN)
      buffer.putDouble(value)

    case AvroLong(value) =>
      Varint.writeSignedLong(value, buffer)

    case AvroBoolean(value) =>
      if (value)
        buffer.put(1: Byte)
      else
        buffer.put(0: Byte)

    case AvroString(value) =>
      writeString(value, buffer)

    case AvroFixed(value, _) =>
      value.foreach(buffer.put)

    case AvroBytes(value) =>
      Varint.writeSignedInt(value.length, buffer)
      value.foreach(buffer.put)

    case AvroUnion(value, typeIndex, _) =>
      Varint.writeSignedLong(typeIndex, buffer)
      encode(value, buffer)

    case AvroArray(values, _) =>
      Varint.writeSignedLong(values.size, buffer)

      if (values.nonEmpty) {
        // TODO It is not tail recursive
        values.foreach(encode(_, buffer))
        Varint.writeSignedLong(0, buffer)
      }

    case AvroMap(entries, _) =>
      Varint.writeSignedLong(entries.size, buffer)

      if (entries.nonEmpty) {
        // TODO It is not tail recursive
        entries.foreach {
          case (k, v) =>
            writeString(k, buffer)
            encode(v, buffer)
        }
        Varint.writeSignedLong(0, buffer)
      }

    case AvroRecord(_, fields) =>
      // TODO It is not tail recursive
      fields.values.foreach(encode(_, buffer))

    case AvroEnum(name, symbolIndex, symbols) =>
      Varint.writeSignedInt(symbolIndex, buffer)

  }

  def encode(avro: Avro[_]): Array[Byte] = {
    val buffer = ByteBuffer.allocate(1024)
    encode(avro, buffer)
    buffer.flip()
    val data = Array.ofDim[Byte](buffer.limit() - buffer.position())
    buffer.get(data)
    data
  }

  def decode[T <: Schema](schema: T, bytes: Array[Byte]): Either[ParsingFailure, Avro[T]] =
    decode(schema, ByteBuffer.wrap(bytes))

  def decode[T <: Schema](schema: T, buffer: ByteBuffer): Either[ParsingFailure, Avro[T]] = schema match {
    case NullSchema =>
      Right(AvroNull.asInstanceOf[Avro[T]])

    case IntSchema =>
      Right(AvroInt(Varint.readSignedInt(buffer)).asInstanceOf[Avro[T]])

    case FloatSchema =>
      Try(
        buffer
          .order(ByteOrder.LITTLE_ENDIAN)
          .asFloatBuffer()
          .get()
      ).fold(error => Left(ParsingFailure(error.getMessage)), ok => Right(AvroFloat(ok).asInstanceOf[Avro[T]]))

    case DoubleSchema =>
      Try(
        buffer
          .order(ByteOrder.LITTLE_ENDIAN)
          .asDoubleBuffer()
          .get()
      ).fold(error => Left(ParsingFailure(error.getMessage)), ok => Right(AvroDouble(ok).asInstanceOf[Avro[T]]))

    case LongSchema =>
      Right(AvroLong(Varint.readSignedLong(buffer)).asInstanceOf[Avro[T]])

    case BooleanSchema =>
      Try(buffer.get()).toEither.left
        .map {
          case NonFatal(e) => ParsingFailure(e.getMessage)
        }
        .right
        .flatMap {
          case 0 => Right(AvroBoolean(false).asInstanceOf[Avro[T]])
          case 1 => Right(AvroBoolean(true).asInstanceOf[Avro[T]])
          case x => Left(ParsingFailure(s"The byte $x is not a valid boolean"))
        }

    case StringSchema =>
      val bytesLength = Varint.readSignedInt(buffer)
      val stringBytes = Array.ofDim[Byte](bytesLength)
      buffer.get(stringBytes)
      Right(AvroString(new String(stringBytes, StandardCharsets.UTF_8)).asInstanceOf[Avro[T]])

    case FixedSchema(length) =>
      if (length > Int.MaxValue) {
        Left(ParsingFailure("Fixed with length > Int.MaxValue is not supported"))
      } else {
        Try {
          val content = Array.ofDim[Byte](length.toInt)
          buffer.get(content)
          content
        }.map(xs => immutable.Seq(xs: _*))
          .map(Avro.fixed(_, length).asInstanceOf[Avro[T]])
          .toEither
          .left
          .map(e => ParsingFailure(e.getMessage))
      }

    case BytesSchema =>
      (for {
        length <- Try(Varint.readSignedLong(buffer))
        if length < Int.MaxValue
        content <- Try {
          val content = Array.ofDim[Byte](length.toInt)
          buffer.get(content)
          content
        }
      } yield content)
        .map(xs => immutable.Seq(xs: _*))
        .map(AvroBytes.apply)
        .map(_.asInstanceOf[Avro[T]])
        .toEither
        .left
        .map(e => ParsingFailure(e.getMessage))

    case UnionSchema(types) =>
      Try(Varint.readSignedLong(buffer)).toEither.left
        .map {
          case NonFatal(e) => ParsingFailure(e.getMessage)
        }
        .flatMap {
          case i if i < Int.MaxValue => Right(i.toInt)
          case _ => Left(ParsingFailure("The index type bigger than Int.MaxSize is not yet supported"))
        }
        .flatMap { index =>
          if (types.isDefinedAt(index)) {
            decode(types(index), buffer).map(AvroUnion(_, index, types).asInstanceOf[Avro[T]])
          } else {
            Left(ParsingFailure(s"The type at index $index does not exist in the union type"))
          }
        }

    case ArraySchema(elementSchema) =>
      // TODO Add multiple blocks array support
      // TODO Handle negative length

      val numberOfElement = Varint.readSignedInt(buffer)
      (0 until numberOfElement).toVector
        .map(_ => decode(elementSchema, buffer))
        .sequence
        .map(xs=> Avro.array(elementSchema, xs:_*).asInstanceOf[Avro[T]])

    case MapSchema(valueSchema) =>
      val mapBuilder = Map.newBuilder[String, Avro[Schema]]

      var numberOfElements = Varint.readSignedLong(buffer)
      while (numberOfElements > 0) {
        var index = 0
        while (index < numberOfElements) {
          val key = readString(buffer)
          val value = decode(valueSchema, buffer).getOrElse(throw new RuntimeException("Error ..."))
          mapBuilder += key -> value
          index += 1
        }
        numberOfElements = Varint.readSignedLong(buffer)
      }

      Right(Avro.map[Schema](valueSchema, mapBuilder.result().toList: _*).asInstanceOf[Avro[T]])

    case RecordSchema(fullName, fields) =>
      fields
        .map {
          case (fieldName, fieldSchema) =>
            decode(fieldSchema, buffer).map(fieldName -> _)
        }
        .view
        .takeWhile(_.isRight)
        .foldLeft(Either.right[ParsingFailure, Vector[(String, Avro[Schema])]](Vector.empty[(String, Avro[Schema])])) { (mxs, mx) =>
          for {
            xs <- mxs
            x <- mx
          } yield xs :+ x
        }
        .map(fields => Avro.record(fullName, fields: _*).asInstanceOf[Avro[T]])

    case EnumSchema(name, symbols) =>
      Try(Varint.readSignedInt(buffer))
        .toEither
        .left
        .map(e => ParsingFailure(e.getMessage))
        .right
        .flatMap {
          case i if i < symbols.length => Right(AvroEnum(name, i, symbols).asInstanceOf[Avro[T]])
          case i => Left(ParsingFailure(s"The symbol index $i is out of range ${symbols.mkString("[", ",", "]")}"))
        }

    case _ => Left(ParsingFailure(s"Schema $schema is not supported"))
  }

  private def readString(buffer: ByteBuffer): String = {
    val bytesLength = Varint.readSignedInt(buffer)
    val stringBytes = Array.ofDim[Byte](bytesLength)
    buffer.get(stringBytes)
    new String(stringBytes, StandardCharsets.UTF_8)
  }

  private def writeString(string: String, buffer: ByteBuffer) = {
    val stringBytes = string.getBytes(UTF_8)
    Varint.writeSignedLong(stringBytes.length, buffer)
    buffer.put(stringBytes)
  }

}
