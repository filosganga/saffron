package com.github.filosganga.cabron

import java.nio.ByteBuffer



object Varint {

  def writeSignedLong(x: Long, dest: ByteBuffer): Unit = {
    // sign to even/odd mapping: http://code.google.com/apis/protocolbuffers/docs/encoding.html#types
    writeUnsignedLong((x << 1) ^ (x >> 63), dest)
  }

  def writeUnsignedLong(v: Long, dest: ByteBuffer): Unit = {
    var x = v
    while((x & 0xFFFFFFFFFFFFFF80L) != 0L) {
      dest put ((x & 0x7F) | 0x80).toByte
      x >>>= 7
    }
    dest put (x & 0x7F).toByte
  }

  def writeSignedInt(x: Int, dest: ByteBuffer): Unit = {
    writeUnsignedInt((x << 1) ^ (x >> 31), dest)
  }

  def writeUnsignedInt(v: Int, dest: ByteBuffer): Unit = {
    var x = v
    while((x & 0xFFFFF80) != 0L) {
      dest put ((x & 0x7F) | 0x80).toByte
      x >>>= 7
    }
    dest put (x & 0x7F).toByte
  }

  def readSignedInt(src: ByteBuffer): Int = {
    val unsigned = readUnsignedInt(src)
    // undo even odd mapping
    val tmp = (((unsigned << 31) >> 31) ^ unsigned) >> 1
    // restore sign
    tmp ^ (unsigned & (1 << 31))
  }

  def readUnsignedInt(src: ByteBuffer): Int = {
    var i = 0
    var v = 0
    var read = 0
    do {
      read = src.get
      v |= (read & 0x7F) << i
      i += 7
      require(i <= 35)
    } while((read & 0x80) != 0)
    v
  }


  def readSignedLong(src: ByteBuffer): Long = {
    val unsigned = readUnsignedLong(src)
    // undo even odd mapping
    val tmp = (((unsigned << 63) >> 63) ^ unsigned) >> 1
    // restore sign
    tmp ^ (unsigned & (1L << 63))
  }

  def readUnsignedLong(src: ByteBuffer): Long = {
    var i = 0
    var v = 0L
    var read = 0L
    do {
      read = src.get
      v |= (read & 0x7F) << i
      i += 7
      require(i <= 70)
    } while((read & 0x80L) != 0)
    v
  }
}