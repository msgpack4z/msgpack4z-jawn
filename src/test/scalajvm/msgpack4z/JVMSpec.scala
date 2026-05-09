package msgpack4z

object Java06Spec extends SpecBase("java06") {
  override protected[this] def packer() = Msgpack06.defaultPacker()
  override protected[this] def unpacker(bytes: Array[Byte]) = Msgpack06.defaultUnpacker(bytes)
}

object JavaSpec extends SpecBase("java") {
  override protected[this] def packer() = new MsgpackJavaPacker()
  override protected[this] def unpacker(bytes: Array[Byte]) = MsgpackJavaUnpacker.defaultUnpacker(bytes)
}
