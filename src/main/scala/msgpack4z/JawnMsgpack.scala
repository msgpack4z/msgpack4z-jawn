package msgpack4z

import scalaz.{-\/, \/, \/-}
import jawn.ast._

object JawnMsgpack {

  def jValueCodec(options: JawnUnpackOptions): MsgpackCodec[JValue] =
    new CodecJawnJValue(options)

  def jArrayCodec(options: JawnUnpackOptions): MsgpackCodec[JArray] =
    new CodecJawnJArray(options)

  def jObjectCodec(options: JawnUnpackOptions): MsgpackCodec[JObject] =
    new CodecJawnJObject(options)

  def allCodec(options: JawnUnpackOptions): (MsgpackCodec[JValue], MsgpackCodec[JArray], MsgpackCodec[JObject]) = (
    jValueCodec(options),
    jArrayCodec(options),
    jObjectCodec(options)
  )

  def jObject2msgpack(packer: MsgPacker, obj: JObject): Unit = {
    packer.packMapHeader(obj.vs.size)
    obj.vs.foreach { field =>
      packer.packString(field._1)
      jValue2msgpack(packer, field._2)
    }
    packer.mapEnd()
  }

  def jArray2msgpack(packer: MsgPacker, array: JArray): Unit = {
    packer.packArrayHeader(array.vs.length)
    var i = 0
    while(i < array.vs.length){
      jValue2msgpack(packer, array.vs(i))
      i += 1
    }
    packer.arrayEnd()
  }

  def jValue2msgpack(packer: MsgPacker, json: JValue): Unit = {
    json match {
      case value: JObject =>
        jObject2msgpack(packer, value)
      case value: JArray =>
        jArray2msgpack(packer, value)
      case JNull =>
        packer.packNil()
      case JTrue =>
        packer.packBoolean(true)
      case JFalse =>
        packer.packBoolean(false)
      case JString(value) =>
        packer.packString(value)
      case LongNum(value) =>
        packer.packLong(value)
      case DoubleNum(value) =>
        packer.packDouble(value)
      case value: DeferLong =>
        packer.packLong(value.asLong)
      case value: DeferNum =>
        packer.packDouble(value.asDouble)
    }
  }

  def msgpack2json(unpacker: MsgUnpacker, unpackOptions: JawnUnpackOptions): UnpackResult[JValue] = {
    val result = Result.empty[JValue]
    if (msgpack2json0(unpacker, result, unpackOptions)) {
      \/-(result.value)
    } else {
      -\/(result.error)
    }
  }

  def msgpack2jsObj(unpacker: MsgUnpacker, unpackOptions: JawnUnpackOptions): UnpackResult[JObject] = {
    val result = Result.empty[JObject]
    if (msgpack2jsObj0(unpacker, result, unpackOptions)) {
      \/-(result.value)
    } else {
      -\/(result.error)
    }
  }

  def msgpack2jsArray(unpacker: MsgUnpacker, unpackOptions: JawnUnpackOptions): UnpackResult[JArray] = {
    val result = Result.empty[JArray]
    if (msgpack2jsArray0(unpacker, result, unpackOptions)) {
      \/-(result.value)
    } else {
      -\/(result.error)
    }
  }

  private[this] final case class Result[A](
    var value: A, var error: UnpackError
  )
  private[this] object Result {
    def fromEither[A](e: UnpackError \/ A, result: Result[A]): Boolean = e match{
      case \/-(r) =>
        result.value = r
        true
      case -\/(l) =>
        result.error = l
        false
    }

    def empty[A >: Null]: Result[A] = Result[A](null, null)
  }

  private[this] def msgpack2jsObj0(unpacker: MsgUnpacker, result: Result[JObject], unpackOptions: JawnUnpackOptions): Boolean = {
    val size = unpacker.unpackMapHeader()
    val obj = collection.mutable.AnyRefMap.empty[String, JValue]
    var i = 0
    val mapElem = Result.empty[JValue]
    var success = true

    def process(key: String): Unit = {
     if (msgpack2json0(unpacker, mapElem, unpackOptions)) {
       obj += ((key, mapElem.value))
       i += 1
     } else {
       result.error = mapElem.error
       success = false
     }
    }

    while (i < size && success) {
      val tpe = unpacker.nextType()
      if(tpe == MsgType.STRING) {
        process(unpacker.unpackString())
      }else{
        unpackOptions.nonStringKey(tpe, unpacker) match {
          case Some(key) =>
            process(key)
          case None =>
            success = false
            result.error = Other("not string key")
        }
      }
    }
    unpacker.mapEnd()
    if (success) {
      result.value = JObject(obj)
    }
    success
  }

  private[this] def msgpack2jsArray0(unpacker: MsgUnpacker, result: Result[JArray], unpackOptions: JawnUnpackOptions): Boolean = {
    val size = unpacker.unpackArrayHeader()
    val array = new Array[JValue](size)
    var i = 0
    val arrayElem = Result[JValue](null, null)
    var success = true
    while (i < size && success) {
      if (msgpack2json0(unpacker, arrayElem, unpackOptions)) {
        array(i) = arrayElem.value
        i += 1
      } else {
        result.error = arrayElem.error
        success = false
      }
    }
    unpacker.arrayEnd()
    if (success) {
      result.value = JArray(array)
    }
    success
  }

  private[this] val BigIntegerLongMax = java.math.BigInteger.valueOf(Long.MaxValue)
  private[this] val BigIntegerLongMin = java.math.BigInteger.valueOf(Long.MinValue)

  private def isValidLong(value: java.math.BigInteger): Boolean =
    (BigIntegerLongMin.compareTo(value) <= 0) && (value.compareTo(BigIntegerLongMax) <= 0)

  private[msgpack4z] def msgpack2json0(unpacker: MsgUnpacker, result: Result[JValue], unpackOptions: JawnUnpackOptions): Boolean = {
    unpacker.nextType match {
      case MsgType.NIL =>
        unpacker.unpackNil()
        result.value = JNull
        true
      case MsgType.BOOLEAN =>
        if (unpacker.unpackBoolean()) {
          result.value = JTrue
        } else {
          result.value = JFalse
        }
        true
      case MsgType.INTEGER =>
        val value = unpacker.unpackBigInteger()
        if(isValidLong(value)){
          result.value = LongNum(value.longValue())
        }else{
          result.value = DeferNum(value.toString)
        }
        true
      case MsgType.FLOAT =>
        val f = unpacker.unpackDouble()
        if(f.isPosInfinity){
          Result.fromEither(unpackOptions.positiveInf, result)
        }else if(f.isNegInfinity){
          Result.fromEither(unpackOptions.negativeInf, result)
        }else if(java.lang.Double.isNaN(f)) {
          Result.fromEither(unpackOptions.nan, result)
        }else{
          result.value = DoubleNum(f)
        }
        true
      case MsgType.STRING =>
        result.value = JString(unpacker.unpackString())
        true
      case MsgType.ARRAY =>
        val result0 = Result.empty[JArray]
        val r = msgpack2jsArray0(unpacker, result0, unpackOptions)
        result.error = result0.error
        result.value = result0.value
        r
      case MsgType.MAP =>
        val result0 = Result.empty[JObject]
        val r = msgpack2jsObj0(unpacker, result0, unpackOptions)
        result.error = result0.error
        result.value = result0.value
        r
      case MsgType.BINARY =>
        Result.fromEither(unpackOptions.binary(unpacker), result)
      case MsgType.EXTENSION =>
        Result.fromEither(unpackOptions.extension(unpacker), result)
    }
  }
}


private final class CodecJawnJArray(unpackOptions: JawnUnpackOptions) extends MsgpackCodecConstant[JArray](
  JawnMsgpack.jArray2msgpack,
  unpacker => JawnMsgpack.msgpack2jsArray(unpacker, unpackOptions)
)

private final class CodecJawnJValue(unpackOptions: JawnUnpackOptions) extends MsgpackCodecConstant[JValue](
  JawnMsgpack.jValue2msgpack,
  unpacker => JawnMsgpack.msgpack2json(unpacker, unpackOptions)
)

private final class CodecJawnJObject(unpackOptions: JawnUnpackOptions) extends MsgpackCodecConstant[JObject](
  JawnMsgpack.jObject2msgpack,
  unpacker => JawnMsgpack.msgpack2jsObj(unpacker, unpackOptions)
)
