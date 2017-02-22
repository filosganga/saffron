package com.github.filosganga.cabron

import org.apache.avro.{LogicalType, LogicalTypes, Schema, SchemaCompatibility}

object AvroJavaTest extends App {

  case class Foo(name: String) extends LogicalType("Foo")

  val parser = new Schema.Parser()
  LogicalTypes.register("Foo", new LogicalTypes.LogicalTypeFactory {
    override def fromSchema(schema: Schema): LogicalType = {
      Foo(schema.getFullName)
    }
  })

  val initialSchema = new Schema.Parser().parse(
    """
      |{
      |  "name": "Event",
      |  "type": "record",
      |  "fields": [
      |     {
      |       "name": "eventId",
      |       "type": "string"
      |     },
      |     {
      |       "name": "createdAt",
      |       "type": "long",
      |       "logicalType": "timestamp-millis"
      |     },
      |     {
      |       "name": "traceToken",
      |       "type": ["null", "string"],
      |       "default": null
      |     },
      |     {
      |       "name": "subclass",
      |       "type": [
      |         "null",
      |         {
      |           "name": "EventOne",
      |           "type": "record",
      |           "fields": [
      |             {
      |               "name": "id",
      |               "type": "string"
      |             },
      |             {
      |               "name": "parent",
      |               "type": ["null", "string"],
      |               "default": null
      |             },
      |             {
      |               "name": "kind",
      |               "type": {
      |                 "type": "enum",
      |                 "name": "Numbers",
      |                 "symbols": ["ONE", "TWO"],
      |                 "default": "ONE"
      |               }
      |             }
      |           ]
      |
      |         }
      |       ],
      |       "default": null
      |     }
      |  ]
      |}
    """.stripMargin)

  val schemaWithAdditionalEnumSymbol = new Schema.Parser().parse(
    """
      |{
      |  "name": "Event",
      |  "type": "record",
      |  "fields": [
      |     {
      |       "name": "eventId",
      |       "type": "string"
      |     },
      |     {
      |       "name": "createdAt",
      |       "type": "long",
      |       "logicalType": "timestamp-millis"
      |     },
      |     {
      |       "name": "traceToken",
      |       "type": ["null", "string"],
      |       "default": null
      |     },
      |     {
      |       "name": "subclass",
      |       "type": [
      |         "null",
      |         {
      |           "name": "EventOne",
      |           "type": "record",
      |           "fields": [
      |             {
      |               "name": "id",
      |               "type": "string"
      |             },
      |             {
      |               "name": "parent",
      |               "type": ["null", "string"],
      |               "default": null
      |             },
      |             {
      |               "name": "kind",
      |               "type": {
      |                 "type": "enum",
      |                 "name": "Numbers",
      |                 "symbols": ["ONE", "TWO", "THREE"],
      |                 "default": "ONE"
      |               }
      |             }
      |           ]
      |
      |         }
      |       ],
      |       "default": null
      |     }
      |  ]
      |}
    """.stripMargin)

  val schemaWithoutEnumSymbol = new Schema.Parser().parse(
    """
      |{
      |  "name": "Event",
      |  "type": "record",
      |  "fields": [
      |     {
      |       "name": "eventId",
      |       "type": "string"
      |     },
      |     {
      |       "name": "createdAt",
      |       "type": "long",
      |       "logicalType": "timestamp-millis"
      |     },
      |     {
      |       "name": "traceToken",
      |       "type": ["null", "string"],
      |       "default": null
      |     },
      |     {
      |       "name": "subclass",
      |       "type": [
      |         "null",
      |         {
      |           "name": "EventOne",
      |           "type": "record",
      |           "fields": [
      |             {
      |               "name": "id",
      |               "type": "string"
      |             },
      |             {
      |               "name": "parent",
      |               "type": ["null", "string"],
      |               "default": null
      |             },
      |             {
      |               "name": "kind",
      |               "type": {
      |                 "type": "enum",
      |                 "name": "Numbers",
      |                 "symbols": ["ONE"],
      |                 "default": "ONE"
      |               }
      |             }
      |           ]
      |
      |         }
      |       ],
      |       "default": null
      |     }
      |  ]
      |}
    """.stripMargin)

  val schemaWithAdditionalField = new Schema.Parser().parse(
    """
      |{
      |  "name": "Event",
      |  "type": "record",
      |  "fields": [
      |     {
      |       "name": "eventId",
      |       "type": "string"
      |     },
      |     {
      |       "name": "createdAt",
      |       "type": "long",
      |       "logicalType": "timestamp-millis"
      |     },
      |     {
      |       "name": "traceToken",
      |       "type": ["null", "string"],
      |       "default": null
      |     },
      |     {
      |       "name": "subclass",
      |       "type": [
      |         "null",
      |         {
      |           "name": "EventOne",
      |           "type": "record",
      |           "fields": [
      |             {
      |               "name": "id",
      |               "type": "string"
      |             },
      |             {
      |               "name": "parent",
      |               "type": ["null", "string"],
      |               "default": null
      |             },
      |             {
      |               "name": "level",
      |               "type": "int"
      |             },
      |             {
      |               "name": "kind",
      |               "type": {
      |                 "type": "enum",
      |                 "name": "Numbers",
      |                 "symbols": ["ONE", "TWO"],
      |                 "default": "ONE"
      |               }
      |             }
      |           ]
      |
      |         }
      |       ],
      |       "default": null
      |     }
      |  ]
      |}
    """.stripMargin)

  val schemaWithoutOptionalField = new Schema.Parser().parse(
    """
      |{
      |  "name": "Event",
      |  "type": "record",
      |  "fields": [
      |     {
      |       "name": "eventId",
      |       "type": "string"
      |     },
      |     {
      |       "name": "createdAt",
      |       "type": "long",
      |       "logicalType": "timestamp-millis"
      |     },
      |     {
      |       "name": "traceToken",
      |       "type": ["null", "string"],
      |       "default": null
      |     },
      |     {
      |       "name": "subclass",
      |       "type": [
      |         "null",
      |         {
      |           "name": "EventOne",
      |           "type": "record",
      |           "fields": [
      |             {
      |               "name": "id",
      |               "type": "string"
      |             },
      |             {
      |               "name": "kind",
      |               "type": {
      |                 "type": "enum",
      |                 "name": "Numbers",
      |                 "symbols": ["ONE", "TWO"],
      |                 "default": "ONE"
      |               }
      |             }
      |           ]
      |
      |         }
      |       ],
      |       "default": null
      |     }
      |  ]
      |}
    """.stripMargin)

  val schemaWithoutMandatoryField = new Schema.Parser().parse(
    """
      |{
      |  "name": "Event",
      |  "type": "record",
      |  "fields": [
      |     {
      |       "name": "eventId",
      |       "type": "string"
      |     },
      |     {
      |       "name": "createdAt",
      |       "type": "long",
      |       "logicalType": "timestamp-millis"
      |     },
      |     {
      |       "name": "traceToken",
      |       "type": ["null", "string"],
      |       "default": null
      |     },
      |     {
      |       "name": "subclass",
      |       "type": [
      |         "null",
      |         {
      |           "name": "EventOne",
      |           "type": "record",
      |           "fields": [
      |             {
      |               "name": "parent",
      |               "type": ["null", "string"],
      |               "default": null
      |             },
      |             {
      |               "name": "kind",
      |               "type": {
      |                 "type": "enum",
      |                 "name": "Numbers",
      |                 "symbols": ["ONE", "TWO"],
      |                 "default": "ONE"
      |               }
      |             }
      |           ]
      |
      |         }
      |       ],
      |       "default": null
      |     }
      |  ]
      |}
    """.stripMargin)

  val schemaWithAdditionalType = new Schema.Parser().parse(
    """
      |{
      |  "name": "Event",
      |  "type": "record",
      |  "fields": [
      |     {
      |       "name": "eventId",
      |       "type": "string"
      |     },
      |     {
      |       "name": "createdAt",
      |       "type": "long",
      |       "logicalType": "timestamp-millis"
      |     },
      |     {
      |       "name": "traceToken",
      |       "type": ["null", "string"],
      |       "default": null
      |     },
      |     {
      |       "name": "subclass",
      |       "type": [
      |         "null",
      |         {
      |           "name": "EventOne",
      |           "type": "record",
      |           "fields": [
      |             {
      |               "name": "id",
      |               "type": "string"
      |             },
      |             {
      |               "name": "parent",
      |               "type": ["null", "string"],
      |               "default": null
      |             },
      |             {
      |               "name": "kind",
      |               "type": {
      |                 "type": "enum",
      |                 "name": "Numbers",
      |                 "symbols": ["ONE", "TWO"],
      |                 "default": "ONE"
      |               }
      |             }
      |           ]
      |         },
      |         {
      |           "name": "EventTwo",
      |           "type": "record",
      |           "fields": [
      |             {
      |               "name": "id",
      |               "type": "string"
      |             },
      |             {
      |               "name": "parent",
      |               "type": ["null", "string"],
      |               "default": null
      |             }
      |           ]
      |         }
      |       ],
      |       "default": null
      |     }
      |  ]
      |}
    """.stripMargin)


  println(SchemaCompatibility.checkReaderWriterCompatibility(initialSchema, schemaWithoutEnumSymbol))

}
