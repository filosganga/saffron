package com.ovoenergy.saffron.core

trait FromAvro[T] {

  def fromAvro(avro: Avro): Either[ParsingFailure, T]

}
