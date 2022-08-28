package cats.xml.codec

import cats.xml.{Xml, XmlData}
import cats.Contravariant
import cats.data.Validated

// T => XML
trait Encoder[T] {

  def encode(t: T): Xml

  def contramap[U](f: U => T): Encoder[U] =
    Encoder.of(f.andThen(encode))
}
object Encoder extends EncoderInstances with EncoderSyntax {

  // lazy due circular dependencies with instances
  lazy val id: Encoder[Xml] = Encoder.of(identity)

  def apply[T: Encoder]: Encoder[T] = implicitly[Encoder[T]]

  def of[T](f: T => Xml): Encoder[T] = (t: T) => f(t)

  def pure[T](ns: => Xml): Encoder[T] = Encoder(_ => ns)
}

// #################### SYNTAX ####################
private[xml] trait EncoderSyntax {

  implicit class EncoderOps[T](t: T) {

    def toXml(implicit e: Encoder[T]): Xml =
      e.encode(t)

    def toXmlWiden[TT >: T](implicit e: Encoder[TT]): Xml =
      e.encode(t)
  }
}

// #################### INSTANCES ####################
private[xml] trait EncoderInstances extends EncoderPrimitivesInstances {

  implicit def codecToEncoder[T: Codec]: Encoder[T] = Codec[T].encoder

  implicit val catsContravariantInstanceForEncoder: Contravariant[Encoder] =
    new Contravariant[Encoder] {
      override def contramap[A, B](fa: Encoder[A])(f: B => A): Encoder[B] = fa.contramap(f)
    }
}
private[xml] trait EncoderPrimitivesInstances {

  implicit val encoderXml: Encoder[Xml] = Encoder.id

  implicit def encoderOption[T: Encoder]: Encoder[Option[T]] =
    Encoder.of[Option[T]] {
      case Some(value) => Encoder[T].encode(value)
      case None        => Xml.Null
    }

  implicit val encoderXmlData: DataEncoder[XmlData]       = DataEncoder.of(identity)
  implicit val encoderUnit: DataEncoder[Unit]             = DataEncoder.of(_ => Xml.Null)
  implicit val encoderString: DataEncoder[String]         = DataEncoder.of(XmlData.fromString(_))
  implicit val encoderChar: DataEncoder[Char]             = DataEncoder.of(XmlData.fromChar)
  implicit val encoderByte: DataEncoder[Byte]             = DataEncoder.of(XmlData.fromByte)
  implicit val encoderBoolean: DataEncoder[Boolean]       = DataEncoder.of(XmlData.fromBoolean)
  implicit val encoderInt: DataEncoder[Int]               = DataEncoder.of(XmlData.fromInt)
  implicit val encoderLong: DataEncoder[Long]             = DataEncoder.of(XmlData.fromLong)
  implicit val encoderFloat: DataEncoder[Float]           = DataEncoder.of(XmlData.fromFloat)
  implicit val encoderDouble: DataEncoder[Double]         = DataEncoder.of(XmlData.fromDouble)
  implicit val encoderBigDecimal: DataEncoder[BigDecimal] = DataEncoder.of(XmlData.fromBigDecimal)
  implicit val encoderBigInt: DataEncoder[BigInt]         = DataEncoder.of(XmlData.fromBigInt)

}

// #################### DATA ENCODER ####################
trait DataEncoder[T] extends Encoder[T] {
  override def encode(t: T): XmlData
  override def contramap[U](f: U => T): DataEncoder[U] =
    DataEncoder.of(f.andThen(encode))
}
object DataEncoder {
  def apply[T: DataEncoder]: DataEncoder[T] = implicitly[DataEncoder[T]]

  def of[T](f: T => XmlData): DataEncoder[T] = (t: T) => f(t)

  def stringParsedEncoder: DataEncoder[String] = {

    def fromValue[T](value: T): Either[DecoderFailure, XmlData] =
      value match {
        case v: String     => Right(XmlData.fromString(v))
        case v: Char       => Right(XmlData.fromChar(v))
        case v: Byte       => Right(XmlData.fromByte(v))
        case v: Boolean    => Right(XmlData.fromBoolean(v))
        case v: Int        => Right(XmlData.fromInt(v))
        case v: Long       => Right(XmlData.fromLong(v))
        case v: Float      => Right(XmlData.fromFloat(v))
        case v: Double     => Right(XmlData.fromDouble(v))
        case v: BigDecimal => Right(XmlData.fromBigDecimal(v))
        case v: BigInt     => Right(XmlData.fromBigInt(v))
        case _             => Left(DecoderFailure.Custom("Cannot decode specified type."))
      }

    DataEncoder.of[String](strValue => {
      Decoder
        .oneOf(
          Decoder.decodeBoolean.emap(fromValue),
          Decoder.decodeInt.emap(fromValue),
          Decoder.decodeLong.emap(fromValue),
          Decoder.decodeFloat.emap(fromValue),
          Decoder.decodeDouble.emap(fromValue),
          Decoder.decodeBigInt.emap(fromValue),
          Decoder.decodeBigDecimal.emap(fromValue),
          Decoder.decodeByte.emap(fromValue),
          Decoder.decodeCharArray.emap(fromValue),
          Decoder.decodeString.emap(fromValue)
        )
        .decode(XmlData.fromString(strValue)) match {
        case Validated.Valid(a)   => a
        case Validated.Invalid(_) => XmlData.fromString(strValue)
      }
    })
  }
}
