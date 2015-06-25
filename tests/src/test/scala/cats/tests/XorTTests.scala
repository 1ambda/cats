package cats.tests

import cats.MonadError
import cats.data.{Xor, XorT}
import cats.laws.discipline.{MonadErrorTests, MonoidKTests, SerializableTests}
import cats.laws.discipline.arbitrary._

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll

import scala.util.{Failure, Success, Try}

class XorTTests extends CatsSuite {
  checkAll("XorT[List, String, Int]", MonadErrorTests[XorT[List, ?, ?], String].monadError[Int, Int, Int])
  checkAll("XorT[List, String, Int]", MonoidKTests[XorT[List, String, ?]].monoidK[Int])
  checkAll("MonadError[XorT[List, ?, ?]]", SerializableTests.serializable(MonadError[XorT[List, ?, ?], String]))

  test("toValidated")(check {
    forAll { (xort: XorT[List, String, Int]) =>
      xort.toValidated.map(_.toXor) == xort.value
    }
  })

  test("withValidated")(check {
    forAll { (xort: XorT[List, String, Int], f: String => Char, g: Int => Double) =>
      xort.withValidated(_.bimap(f, g)) == xort.bimap(f, g)
    }
  })

  test("fromTryCatch catches matching exceptions") {
    assert(XorT.fromTryCatch[Option, NumberFormatException]("foo".toInt).isInstanceOf[XorT[Option, NumberFormatException, Int]])
  }

  test("fromTryCatch lets non-matching exceptions escape") {
    val _ = intercept[NumberFormatException] {
      XorT.fromTryCatch[Option, IndexOutOfBoundsException]{ "foo".toInt }
    }
  }

  test("fromXor")(check {
    forAll { (xor: Xor[String, Int]) =>
      Some(xor.isLeft) == XorT.fromXor[Option](xor).isLeft
    }
  })

  test("fromTry")(check {
    forAll { (t: Try[Int]) =>
      Some(t.isFailure) == XorT.fromTry[Option](t).isLeft
    }
  })
}
