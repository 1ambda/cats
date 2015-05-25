package cats
package tests

import cats.arrow.Arrow
import cats.data.Xor
import cats.functor.ProChoice
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._

class FunctionTests extends CatsSuite {
  checkAll("Function0[Int]", ComonadTests[Function0].comonad[Int, Int, Int])
  checkAll("Comonad[Function0]", SerializableTests.serializable(Comonad[Function0]))

  checkAll("Function0[Int]", MonadTests[Function0].monad[Int, Int, Int])
  checkAll("Monad[Function0]", SerializableTests.serializable(Monad[Function0]))

  checkAll("Function1[Int, Int]", ArrowTests[Function1].arrow[Int, Int, Int, Int, Int, Int])
  checkAll("Arrow[Function1]", SerializableTests.serializable(Arrow[Function1]))

  implicit val function1XorEq: Eq[(Int Xor Int) => (Int Xor Int)] =
    function1Eq(xorArbitrary[Int, Int], Xor.xorEq[Int, Int])

  checkAll("Function1[Int, Int]", ProChoiceTests[Function1].prochoice[Int, Int, Int, Int, Int, Int])
  checkAll("ProChoice[Function1]", SerializableTests.serializable(ProChoice[Function1]))
}
