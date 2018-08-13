package com.ovoenergy.saffron.core

trait ToAvro[T] {

  // TODO Can we infer the schema Type? Maybe as a dependent type of schemaFor
  def toAvro(t: T)(implicit schemaFor: SchemaFor[T]): Avro[_]

}
