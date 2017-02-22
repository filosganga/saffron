package com.ovoenergy.arvo

import java.nio.charset.StandardCharsets

import com.ovoenergy.arvo.ast._
import com.ovoenergy.arvo.ast.Schema._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

class SchemaResolverTest extends WordSpec with Matchers with PropertyChecks {

  import SchemaResolverInstances._

  "Resolver" when {
    "source is Int" when {

      "target is Int" should {
        "keep the source as is" in {
          SchemaResolver.resolve(AvroInt(5), IntSchema) should be(Avro.int(5))
        }
      }

      "target is Long" should {
        "convert the source to Long" in {
          SchemaResolver.resolve(AvroInt(5), LongSchema) should be(Avro.long(5L))
        }
      }

      "target is Float" should {
        "convert the source to Double" in {
          SchemaResolver.resolve(AvroInt(5), FloatSchema) should be(Avro.float(5))
        }
      }

      "target is Double" should {
        "convert the source to Double" in {
          SchemaResolver.resolve(AvroInt(5), DoubleSchema) should be(Avro.double(5.00))
        }
      }
    }

    "source is Long" when {

      "target is Long" should {
        "keep the source as is" in {
          SchemaResolver.resolve(AvroLong(5), LongSchema) should be(Avro.long(5))
        }
      }

      "target is Float" should {
        "convert the source to Float" in {
          SchemaResolver.resolve(AvroLong(5L), FloatSchema) should be(Avro.float(5))
        }
      }

      "target is Double" should {
        "convert the source to Double" in {
          SchemaResolver.resolve(AvroLong(5L), DoubleSchema) should be(Avro.double(5))
        }
      }
    }

    "source is Float" when {

      "target is Float" should {
        "keep the source as is" in {
          SchemaResolver.resolve(AvroFloat(5), FloatSchema) should be(Avro.float(5))
        }
      }

      "target is Double" should {
        "convert the source to Double" in {
          SchemaResolver.resolve(AvroFloat(5), DoubleSchema) should be(Avro.double(5))
        }
      }
    }

    "source is String" when {

      "target is String" should {
        "keep the source as is" in {
          SchemaResolver.resolve(AvroString("Hello Avro"), StringSchema) should be(Avro.string("Hello Avro"))
        }
      }

      "target is Bytes" should {
        "convert the source to Bytes" in {
          SchemaResolver.resolve(AvroString("Hello Avro"), BytesSchema) should be(Avro.bytes("Hello Avro".getBytes(StandardCharsets.UTF_8)))
        }
      }
    }

    "source is Bytes" when {

      "target is Bytes" should {
        "keep the source as is" in {
          SchemaResolver.resolve(Avro.bytes("Hello Avro".getBytes(StandardCharsets.UTF_8)), BytesSchema) should be(Avro.bytes("Hello Avro".getBytes(StandardCharsets.UTF_8)))
        }
      }

      "target is String" should {
        "convert the source to String" in {
          SchemaResolver.resolve(Avro.bytes("Hello Avro".getBytes(StandardCharsets.UTF_8)), StringSchema) should be(Avro.string("Hello Avro"))
        }
      }
    }
  }
}
