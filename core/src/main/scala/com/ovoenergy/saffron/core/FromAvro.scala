package com.ovoenergy.saffron.core

trait FromAvro[T] {

  def fromAvro(avro: Avro[_]): Either[ParsingFailure, T]

}
