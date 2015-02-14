package cats
package laws
package discipline

import algebra.laws._
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

object SemigroupKTests {
  def apply[F[_]: ArbitraryK, A: Arbitrary](implicit eqfa: Eq[F[A]]): SemigroupKTests[F, A] =
    new SemigroupKTests[F, A] {
      def EqFA = eqfa
      def ArbA = implicitly[Arbitrary[A]]
      def ArbF = implicitly[ArbitraryK[F]]
    }
}

trait SemigroupKTests[F[_], A] extends Laws {
  implicit def EqFA: Eq[F[A]]
  implicit def ArbA: Arbitrary[A]
  implicit def ArbF: ArbitraryK[F]
  implicit def ArbFA: Arbitrary[F[A]] = ArbF.synthesize[A](ArbA)

  def associative(implicit F: SemigroupK[F]) = {
    val laws = SemigroupKLaws[F]
    new SemigroupKProperties(
      name = "semigroupK",
      parents = Nil,
      "associative" -> forAll { (a: F[A], b: F[A], c: F[A]) =>
        val (lhs, rhs) = laws.associative(a, b, c)
        lhs ?== rhs
      })
  }

  class SemigroupKProperties(
    val name: String,
    val parents: Seq[SemigroupKProperties],
    val props: (String, Prop)*
  ) extends RuleSet {
    val bases = Nil
  }
}
