package com.ovoenergy.saffron.core

trait ToAvro[T] {

  def toAvro(t: T): Avro

}
