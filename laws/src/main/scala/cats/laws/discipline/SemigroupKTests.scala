package cats
package laws
package discipline

import org.scalacheck.Prop._
import org.scalacheck.Arbitrary
import org.typelevel.discipline.Laws

trait SemigroupKTests[F[_]] extends SerializableTests {
  def laws: SemigroupKLaws[F]

  def semigroupK[A: Arbitrary](implicit
    ArbF: ArbitraryK[F],
    EqFA: Eq[F[A]]
  ): RuleSet = {
    implicit def ArbFA: Arbitrary[F[A]] = ArbF.synthesize[A]

    new RuleSet {
      val name = "semigroupK"
      val bases = Nil
      val parents = Seq(serializable[F[A]])
      val props = Seq(
        "associative" -> forAll(laws.associative[A] _)
      )
    }
  }
}

object SemigroupKTests {
  def apply[F[_]: SemigroupK]: SemigroupKTests[F] =
    new SemigroupKTests[F] { def laws = SemigroupKLaws[F] }
}
