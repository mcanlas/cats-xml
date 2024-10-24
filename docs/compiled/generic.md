# Encoder and Decoder derivation

At the moment supported only for Scala 2.

```sbt
libraryDependencies += "com.github.geirolz" %% "cats-xml-generic" % "0.0.18"
```     
                    
## XmlTypeInterpreter
`XmlTypeInterpreter` is used to map fields and get the xml type and label.
By default using `XmlTypeInterpreter.default[T]` or using it implicitly
- `Attribute`:
  - type is primitive
  - type is a primitive wrapper (BigInt, BigDecimal)
  - type is a value class
- `Text`
  - no fields are treated as `Text` 
- `Child`
  - If it is not neither `Attribute` nor `Text`

## Derivation

Given 
```scala
case class ValueClass(value: String) extends AnyVal
case class Bar(field1: String, field2: BigDecimal)
case class Foo(
  primitiveField: Double = 666d,
  valueClass: ValueClass,
  bar: Bar,
  missingField: Option[String],
  missingNode: Option[Bar]
)
```

### Decoder semiauto

```scala
import cats.xml.XmlNode
import cats.xml.codec.Decoder
import cats.xml.generic.{XmlElemType, XmlTypeInterpreter}

import cats.xml.syntax.*
import cats.xml.generic.decoder.semiauto.*

implicit val typeInterpreterFoo: XmlTypeInterpreter[Foo] =
  XmlTypeInterpreter
          .default[Foo]
          .overrideType(
            _.param(_.valueClass) -> XmlElemType.Attribute
          )
// typeInterpreterFoo: XmlTypeInterpreter[Foo] = cats.xml.generic.XmlTypeInterpreter$$anon$1@3adc38c7

implicit val decoderValueClass: Decoder[ValueClass] = deriveDecoder[ValueClass]
// decoderValueClass: Decoder[ValueClass] = cats.xml.codec.Decoder$$anonfun$of$2@78a62633
implicit val decoderBar: Decoder[Bar] = deriveDecoder[Bar]
// decoderBar: Decoder[Bar] = cats.xml.codec.Decoder$$anonfun$of$2@2d8b4916
implicit val decoderFoo: Decoder[Foo] = deriveDecoder[Foo]
// decoderFoo: Decoder[Foo] = cats.xml.codec.Decoder$$anonfun$of$2@253a6def

XmlNode("foo")
        .withAttrs(
          "primitiveField" := 1d,
          "valueClass" := "TEST"
        )
        .withChildren(
          XmlNode("bar")
                  .withAttrs(
                    "field1" := "BHO",
                    "field2" := BigDecimal(100)
                  )
        )
        .as[Foo]
// res1: Decoder.Result[Foo] = Valid(Foo(1.0,ValueClass(TEST),Bar(BHO,100),None,None))
```

### Encoder semiauto
```scala
import cats.xml.codec.Encoder
import cats.xml.generic.{XmlElemType, XmlTypeInterpreter}

import cats.xml.syntax.*
import cats.xml.generic.encoder.semiauto.*

implicit val typeInterpreterFoo: XmlTypeInterpreter[Foo] =
  XmlTypeInterpreter
    .default[Foo]
    .overrideType(
      _.param(_.valueClass) -> XmlElemType.Attribute
    )
// typeInterpreterFoo: XmlTypeInterpreter[Foo] = cats.xml.generic.XmlTypeInterpreter$$anon$1@372555cd

implicit val encoderValueClass: Encoder[ValueClass] = deriveEncoder[ValueClass]
// encoderValueClass: Encoder[ValueClass] = cats.xml.codec.DataEncoder$$anonfun$of$4@409179c6
implicit val encoderBar: Encoder[Bar]               = deriveEncoder[Bar]
// encoderBar: Encoder[Bar] = cats.xml.codec.Encoder$$anonfun$of$2@3632212e
implicit val encoderFoo: Encoder[Foo]               = deriveEncoder[Foo]
// encoderFoo: Encoder[Foo] = cats.xml.codec.Encoder$$anonfun$of$2@1370cb6c

Foo(
  primitiveField = 1d,
  valueClass     = ValueClass("TEST"),
  bar            = Bar("BHO", BigDecimal(100)),
  missingField   = None,
  missingNode    = None
).toXml
// res2: cats.xml.Xml = <Foo primitiveField="1.0" valueClass="TEST">
//  <Bar field1="BHO" field2="100"/>
// </Foo>
```
