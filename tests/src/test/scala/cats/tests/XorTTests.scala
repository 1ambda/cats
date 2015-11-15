package cats
package tests

import cats.functor.Bifunctor
import cats.data.{Xor, XorT}
import cats.laws.discipline.{BifunctorTests, FoldableTests, FunctorTests, MonadErrorTests, MonoidKTests, SerializableTests, TraverseTests}
import cats.laws.discipline.arbitrary._


class XorTTests extends CatsSuite {
  implicit val eq0 = XorT.xorTEq[List, String, String Xor Int]
  implicit val eq1 = XorT.xorTEq[XorT[List, String, ?], String, Int](eq0)
  checkAll("XorT[List, String, Int]", MonadErrorTests[XorT[List, String, ?], String].monadError[Int, Int, Int])
  checkAll("MonadError[XorT[List, ?, ?]]", SerializableTests.serializable(MonadError[XorT[List, String, ?], String]))
  checkAll("XorT[List, String, Int]", MonoidKTests[XorT[List, String, ?]].monoidK[Int])
  checkAll("MonoidK[XorT[List, String, ?]]", SerializableTests.serializable(MonoidK[XorT[List, String, ?]]))
  checkAll("XorT[List, ?, ?]", BifunctorTests[XorT[List, ?, ?]].bifunctor[Int, Int, Int, String, String, String])
  checkAll("Bifunctor[XorT[List, ?, ?]]", SerializableTests.serializable(Bifunctor[XorT[List, ?, ?]]))
  checkAll("XorT[List, Int, ?]", TraverseTests[XorT[List, Int, ?]].foldable[Int, Int])
  checkAll("Traverse[XorT[List, Int, ?]]", SerializableTests.serializable(Traverse[XorT[List, Int, ?]]))

  {
    implicit val F = ListWrapper.foldable
    checkAll("XorT[ListWrapper, Int, ?]", FoldableTests[XorT[ListWrapper, Int, ?]].foldable[Int, Int])
    checkAll("Foldable[XorT[ListWrapper, Int, ?]]", SerializableTests.serializable(Foldable[XorT[ListWrapper, Int, ?]]))
  }

  {
    implicit val F = ListWrapper.functor
    checkAll("XorT[ListWrapper, Int, ?]", FunctorTests[XorT[ListWrapper, Int, ?]].functor[Int, Int, Int])
    checkAll("Functor[XorT[ListWrapper, Int, ?]]", SerializableTests.serializable(Functor[XorT[ListWrapper, Int, ?]]))
  }

  // make sure that the Monad and Traverse instances don't result in ambiguous
  // Functor instances
  Functor[XorT[List, Int, ?]]

  test("toValidated") {
    forAll { (xort: XorT[List, String, Int]) =>
      xort.toValidated.map(_.toXor) should === (xort.value)
    }
  }

  test("withValidated") {
    forAll { (xort: XorT[List, String, Int], f: String => Char, g: Int => Double) =>
      xort.withValidated(_.bimap(f, g)) should === (xort.bimap(f, g))
    }
  }

  test("fromXor") {
    forAll { (xor: Xor[String, Int]) =>
      Some(xor.isLeft) should === (XorT.fromXor[Option](xor).isLeft)
    }
  }

  test("isLeft negation of isRight") {
    forAll { (xort: XorT[List, String, Int]) =>
      xort.isLeft should === (xort.isRight.map(! _))
    }
  }

  test("double swap is noop") {
    forAll { (xort: XorT[List, String, Int]) =>
      xort.swap.swap should === (xort)
    }
  }

  test("swap negates isRight") {
    forAll { (xort: XorT[List, String, Int]) =>
      xort.swap.isRight should === (xort.isRight.map(! _))
    }
  }

  test("toOption on Right returns Some") {
    forAll { (xort: XorT[List, String, Int]) =>
      xort.toOption.isDefined should === (xort.isRight)
    }
  }

  test("toEither preserves isRight") {
    forAll { (xort: XorT[List, String, Int]) =>
      xort.toEither.map(_.isRight) should === (xort.isRight)
    }
  }

  test("recover recovers handled values") {
    val xort = XorT.left[Id, String, Int]("xort")
    xort.recover { case "xort" => 5 }.isRight should === (true)
  }

  test("recover ignores unhandled values") {
    val xort = XorT.left[Id, String, Int]("xort")
    xort.recover { case "notxort" => 5 } should === (xort)
  }

  test("recover ignores the right side") {
    val xort = XorT.right[Id, String, Int](10)
    xort.recover { case "xort" => 5 } should === (xort)
  }

  test("recoverWith recovers handled values") {
    val xort = XorT.left[Id, String, Int]("xort")
    xort.recoverWith { case "xort" => XorT.right[Id, String, Int](5) }.isRight should === (true)
  }

  test("recoverWith ignores unhandled values") {
    val xort = XorT.left[Id, String, Int]("xort")
    xort.recoverWith { case "notxort" => XorT.right[Id, String, Int](5) } should === (xort)
  }

  test("recoverWith ignores the right side") {
    val xort = XorT.right[Id, String, Int](10)
    xort.recoverWith { case "xort" => XorT.right[Id, String, Int](5) } should === (xort)
  }

  test("transform consistent with value.map") {
    forAll { (xort: XorT[List, String, Int], f: String Xor Int => Long Xor Double) =>
      xort.transform(f) should === (XorT(xort.value.map(f)))
    }
  }

  test("subflatMap consistent with value.map+flatMap") {
    forAll { (xort: XorT[List, String, Int], f: Int => String Xor Double) =>
      xort.subflatMap(f) should === (XorT(xort.value.map(_.flatMap(f))))
    }
  }
}
