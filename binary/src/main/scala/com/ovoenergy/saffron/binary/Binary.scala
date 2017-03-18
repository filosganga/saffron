package com.ovoenergy.saffron.binary

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.saffron.core.Schema._
import com.ovoenergy.saffron.core._

import scala.util.{Failure, Try}
import scala.collection.immutable
import scala.util.control.NonFatal
import cats._
import cats.syntax.all._
import cats.instances.all._

object Binary {

  def encode(avro: Avro, buffer: ByteBuffer): Unit = avro match {
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

    case AvroFixed(_, value) =>
      value.foreach(buffer.put)

    case AvroBytes(value) =>
      Varint.writeSignedInt(value.length, buffer)
      value.foreach(buffer.put)

    case AvroUnion(types, typeIndex, value) =>
      Varint.writeSignedLong(typeIndex, buffer)
      encode(value, buffer)

    case AvroArray(_, values) =>
      Varint.writeSignedLong(values.size, buffer)

      if (values.nonEmpty) {
        // TODO It is not tail recursive
        values.foreach(encode)
        Varint.writeSignedLong(0, buffer)
      }

    case AvroMap(_, entries) =>
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

    case AvroRecord(_, values) =>
      // TODO It is not tail recursive
      values.map(_._2).foreach(encode(_, buffer))

  }

  def encode(avro: Avro): Array[Byte] = {
    val buffer = ByteBuffer.allocate(1024)
    encode(avro, buffer)
    buffer.flip()
    val data = Array.ofDim[Byte](buffer.limit() - buffer.position())
    buffer.get(data)
    data
  }

  def decode(schema: Schema, bytes: Array[Byte]): Either[ParsingFailure, Avro] =
    decode(schema, ByteBuffer.wrap(bytes))

  def decode(schema: Schema, buffer: ByteBuffer): Either[ParsingFailure, Avro] = schema match {
    case NullSchema =>
      Right(AvroNull)

    case IntSchema =>
      Right(AvroInt(Varint.readSignedInt(buffer)))

    case FloatSchema =>
      Try(
        buffer
          .order(ByteOrder.LITTLE_ENDIAN)
          .asFloatBuffer()
          .get()
      ).fold(error => Left(ParsingFailure(error.getMessage)), ok => Right(AvroFloat(ok)))

    case DoubleSchema =>
      Try(
        buffer
          .order(ByteOrder.LITTLE_ENDIAN)
          .asDoubleBuffer()
          .get()
      ).fold(error => Left(ParsingFailure(error.getMessage)), ok => Right(AvroDouble(ok)))

    case LongSchema =>
      Right(AvroLong(Varint.readSignedLong(buffer)))

    case BooleanSchema =>
      Try(buffer.get()).toEither.left
        .map {
          case NonFatal(e) => ParsingFailure(e.getMessage)
        }
        .right
        .flatMap {
          case 0 => Right(AvroBoolean(false))
          case 1 => Right(AvroBoolean(true))
          case x => Left(ParsingFailure(s"The byte $x is not a valid boolean"))
        }

    case StringSchema =>
      val bytesLength = Varint.readSignedInt(buffer)
      val stringBytes = Array.ofDim[Byte](bytesLength)
      buffer.get(stringBytes)
      Right(AvroString(new String(stringBytes, StandardCharsets.UTF_8)))

    case FixedSchema(length) =>
      if (length > Int.MaxValue) {
        Left(ParsingFailure("Fixed with length > Int.MaxValue is not supported"))
      } else {
        Try {
          val content = Array.ofDim[Byte](length.toInt)
          buffer.get(content)
          content
        }.map(xs => immutable.Seq(xs: _*))
          .map(AvroFixed(length, _))
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
            Right(types(index))
          } else {
            Left(ParsingFailure(s"The type at index $index does not exist in the union type"))
          }
        }
        .flatMap { schema =>
          decode(schema, buffer)
        }

    case ArraySchema(elementSchema) =>
      // TODO Add multiple blocks array support
      // TODO Handle negative length

      val numberOfElement = Varint.readSignedInt(buffer)
      (0 until numberOfElement).toVector
        .map(_ => decode(elementSchema, buffer))
        .sequenceU
        .map(AvroArray(elementSchema, _))

    case MapSchema(valueSchema) =>
      val mapBuilder = Map.newBuilder[String, Avro]

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

      Right(AvroMap(valueSchema, mapBuilder.result()))

    case RecordSchema(fullName, fields) =>
      fields
        .map {
          case (fieldName, fieldSchema) =>
            decode(fieldSchema, buffer).map(fieldName -> _)
        }
        .view
        .takeWhile(_.isRight)
        .foldLeft(Either.right[ParsingFailure, Vector[(String, Avro)]](Vector.empty[(String, Avro)])) { (mxs, mx) =>
          for {
            xs <- mxs
            x <- mx
          } yield xs :+ x
        }
        .map(fields => Avro.record(fullName, fields: _*))

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
