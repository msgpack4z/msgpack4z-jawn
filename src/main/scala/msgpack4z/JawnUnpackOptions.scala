package msgpack4z

import jawn.ast.{JNull, LongNum, JArray, JValue}
import msgpack4z.JawnUnpackOptions.NonStringKeyHandler
import scalaz.{\/-, -\/}

final case class JawnUnpackOptions(
  extended: Unpacker[JValue],
  binary: Unpacker[JValue],
  positiveInf: UnpackResult[JValue],
  negativeInf: UnpackResult[JValue],
  nan: UnpackResult[JValue],
  nonStringKey: NonStringKeyHandler
)

object JawnUnpackOptions {
  val binaryToNumberArray: Binary => JValue = { bytes =>
    val array = new Array[JValue](bytes.value.length)
    var i = 0
    while(i < array.length){
      array(i) = LongNum(bytes.value(i))
      i += 1
    }
    JArray(array)
  }

  val binaryToNumberArrayUnpacker: Unpacker[JValue] = { unpacker =>
    CodecInstances.binary.binaryCodec.unpack(unpacker).map(binaryToNumberArray)
  }

  type NonStringKeyHandler = (MsgType, MsgUnpacker) => Option[String]

  private[this] val jNullRight = \/-(JNull)

  val default: JawnUnpackOptions = JawnUnpackOptions(
    _ => -\/(Err(new Exception("does not support extended type"))),
    binaryToNumberArrayUnpacker,
    jNullRight,
    jNullRight,
    jNullRight,
    {case (tpe, unpacker) =>
      PartialFunction.condOpt(tpe){
        case MsgType.NIL =>
          "null"
        case MsgType.BOOLEAN =>
          unpacker.unpackBoolean().toString
        case MsgType.INTEGER =>
          unpacker.unpackBigInteger().toString
        case MsgType.FLOAT =>
          unpacker.unpackDouble().toString
        case MsgType.STRING =>
          unpacker.unpackString()
      }
    }
  )

}
