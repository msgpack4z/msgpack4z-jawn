package msgpack4z

import jawn.ast._
import msgpack4z.JawnUnpackOptions.NonStringKeyHandler
import scalaz.\/-

final case class JawnUnpackOptions(
  extension: Unpacker[JValue],
  binary: Unpacker[JValue],
  positiveInf: UnpackResult[JValue],
  negativeInf: UnpackResult[JValue],
  nan: UnpackResult[JValue],
  nonStringKey: NonStringKeyHandler
)

object JawnUnpackOptions {
  private[this] def bytes2NumberArray(bytes: Array[Byte]): JValue = {
    val array = new Array[JValue](bytes.length)
    var i = 0
    while(i < array.length){
      array(i) = LongNum(bytes(i))
      i += 1
    }
    JArray(array)
  }

  val binaryToNumberArray: Binary => JValue = { bytes =>
    bytes2NumberArray(bytes.value)
  }

  val binaryToNumberArrayUnpacker: Unpacker[JValue] = { unpacker =>
    CodecInstances.binary.binaryCodec.unpack(unpacker).map(binaryToNumberArray)
  }

  val extUnpacker: Unpacker[JValue] = { unpacker =>
    val header = unpacker.unpackExtTypeHeader
    val data = unpacker.readPayload(header.getLength)
    val result = JObject(collection.mutable.Map(
      ("type", LongNum(header.getType)),
      ("data", bytes2NumberArray(data))
    ))
    \/-(result)
  }

  type NonStringKeyHandler = (MsgType, MsgUnpacker) => Option[String]

  private[this] val jNullRight = \/-(JNull)

  val default: JawnUnpackOptions = JawnUnpackOptions(
    extUnpacker,
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
