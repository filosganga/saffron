package com.ovoenergy.saffron.binary

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.charset.StandardCharsets.UTF_8

import com.ovoenergy.saffron.core.Schema._
import com.ovoenergy.saffron.core._

import scala.util.{Failure, Try}
import scala.collection.immutable
import scala.util.control.NonFatal
import cats.syntax.all._
import cats.instances.all._
import scodec.{Attempt, Codec, Encoder, Err, GenCodec}
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

object Binary {

  private val vintZigZag: Codec[Int] =
    vint.xmap[Int](x => (x << 1) ^ (x >> 31), a => (((a << 31) >> 31) ^ a) >> 1 ^ (a & (1 << 31)))

  private val vlongZigZag: Codec[Long] =
    vlong.xmap(a => (a << 1) ^ (a >> 63), a => (((a << 63) >> 63) ^ a) >> 1 ^ (a & (1L << 63)))

  val avroNull: Codec[AvroNull.type] = constant(BitVector.empty).xmap[AvroNull.type](_ => AvroNull, _ => () )
  val avroInt: Codec[AvroInt] = vintZigZag.as[AvroInt]
  val avroLong: Codec[AvroLong] = vlongZigZag.as[AvroLong]
  val avroString: Codec[AvroString] =
    (vlongZigZag.xmap[Int](_.toInt, _.toLong) ~ utf8).xmap[String](_._2, a => a.length -> a).as[AvroString]
  val avroBoolean: Codec[AvroBoolean] =
    byte.xmap[Boolean](_ == 1, a => if (a) 1 else 0).xmap[AvroBoolean](AvroBoolean(_), _.value)
  val avroFloat: Codec[AvroFloat] = floatL.as[AvroFloat]
  val avroDouble: Codec[AvroDouble] = doubleL.as[AvroDouble]
  val avroBytes: Codec[AvroBytes] =
    bytes.xmap[Seq[Byte]](_.toSeq, ByteVector(_: _*)).xmap[immutable.Seq[Byte]](_.toList, identity).as[AvroBytes]
  val avroFixed: Codec[AvroFixed] =
    bytes.xmap[immutable.Seq[Byte]](_.toSeq.toList, ByteVector(_: _*)).as[AvroFixed]

  def avroEnum(schema: EnumSchema): Codec[AvroEnum] =
    vintZigZag.xmap[AvroEnum](a => AvroEnum(a, schema), a => a.symbolIndex)

  def codecForSchema[S <: Schema](schema: S): Codec[Avro[S]] = schema match {
    case NullSchema =>
      avroNull.widen[Avro[S]](a => a.asInstanceOf[Avro[S]], a => Attempt.successful(a.asInstanceOf[AvroNull.type]))
    case IntSchema =>
      avroInt.widen[Avro[S]](a => a.asInstanceOf[Avro[S]], a => Attempt.successful(a.asInstanceOf[AvroInt]))
    case LongSchema =>
      avroLong.widen[Avro[S]](a => a.asInstanceOf[Avro[S]], a => Attempt.successful(a.asInstanceOf[AvroLong]))
    case FloatSchema =>
      avroFloat.widen[Avro[S]](a => a.asInstanceOf[Avro[S]], a => Attempt.successful(a.asInstanceOf[AvroFloat]))
    case DoubleSchema =>
      avroDouble.widen[Avro[S]](a => a.asInstanceOf[Avro[S]], a => Attempt.successful(a.asInstanceOf[AvroDouble]))
    case StringSchema =>
      avroString.widen[Avro[S]](a => a.asInstanceOf[Avro[S]], a => Attempt.successful(a.asInstanceOf[AvroString]))
    case s: EnumSchema =>
      avroEnum(s).widen[Avro[S]](a => a.asInstanceOf[Avro[S]], a => Attempt.successful(a.asInstanceOf[AvroEnum]))
    case FixedSchema(l) =>


  }

}
