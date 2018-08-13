package com.ovoenergy.saffron.core

trait SchemaFor[T] {

  def schemaFor(t: T): Schema

}
