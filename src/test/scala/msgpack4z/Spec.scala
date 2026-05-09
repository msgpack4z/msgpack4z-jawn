package msgpack4z

import org.typelevel.jawn.ast._
import org.scalacheck.{Arbitrary, Gen, Prop, Properties}
import scalaz.{-\/, \/-}

abstract class SpecBase(name: String) extends Properties(name) {

  private val jValuePrimitivesArb: Arbitrary[JValue] =
    Arbitrary(
      Gen.oneOf(
        Gen.const(JNull),
        Gen.const(JTrue),
        Gen.const(JFalse),
        gen[Double].map(a => DeferNum(a.toString)),
        gen[Long].map(LongNum(_)),
        gen[Double].map(DoubleNum(_)),
        gen[String].map(JString.apply)
      )
    )

  private val jsObjectArb1: Arbitrary[JObject] =
    Arbitrary(
      Gen
        .choose(0, 6)
        .flatMap(n =>
          Gen
            .listOfN(
              n,
              Arbitrary
                .arbTuple2(
                  arb[String],
                  jValuePrimitivesArb
                )
                .arbitrary
            )
            .map(JObject.fromSeq)
        )
    )

  private val jsArrayArb1: Arbitrary[JArray] =
    Arbitrary(Gen.choose(0, 6).flatMap(n => Gen.listOfN(n, jValuePrimitivesArb.arbitrary).map(JArray.fromSeq)))

  implicit val jValueArb: Arbitrary[JValue] =
    Arbitrary(
      Gen.oneOf(
        jValuePrimitivesArb.arbitrary,
        jsObjectArb1.arbitrary,
        jsArrayArb1.arbitrary
      )
    )

  implicit val jsObjectArb: Arbitrary[JObject] =
    Arbitrary(
      Gen
        .choose(0, 6)
        .flatMap(n =>
          Gen
            .listOfN(
              n,
              Arbitrary.arbTuple2(arb[String], jValueArb).arbitrary
            )
            .map(JObject.fromSeq)
        )
    )

  implicit val jsArrayArb: Arbitrary[JArray] =
    Arbitrary(Gen.choose(0, 6).flatMap(n => Gen.listOfN(n, jValueArb.arbitrary).map(JArray.fromSeq)))

  final def gen[A: Arbitrary]: Gen[A] =
    implicitly[Arbitrary[A]].arbitrary

  final def arb[A: Arbitrary]: Arbitrary[A] =
    implicitly[Arbitrary[A]]

  protected[this] def packer(): MsgPacker
  protected[this] def unpacker(bytes: Array[Byte]): MsgUnpacker

  private def checkRoundTripBytes[A](implicit A: MsgpackCodec[A], G: Arbitrary[A]) =
    Prop.forAll { (a: A) =>
      A.roundtrip(a, packer(), unpacker _) match {
        case None =>
          true
        case Some(\/-(b)) =>
          println("fail roundtrip bytes " + a + " " + b)
          false
        case Some(-\/(e)) =>
          println(e)
          false
      }
    }

  property("JValue") = {
    implicit val instance = JawnMsgpack.jValueCodec(
      JawnUnpackOptions.default
    )
    checkRoundTripBytes[JValue]
  }

  property("JObject") = {
    implicit val instance = JawnMsgpack.jObjectCodec(
      JawnUnpackOptions.default
    )
    checkRoundTripBytes[JObject]
  }

  property("JArray") = {
    implicit val instance = JawnMsgpack.jArrayCodec(
      JawnUnpackOptions.default
    )
    checkRoundTripBytes[JArray]
  }
}

object NativeSpec extends SpecBase("native") {
  override protected[this] def packer() = MsgOutBuffer.create()
  override protected[this] def unpacker(bytes: Array[Byte]) = MsgInBuffer(bytes)
}
