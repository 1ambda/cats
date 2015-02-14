package cats.laws

import cats.CoFlatMap
import cats.data.Cokleisli
import cats.syntax.coflatMap._

/**
 * Laws that must be obeyed by any [[CoFlatMap]].
 */
trait CoFlatMapLaws[F[_]] extends FunctorLaws[F] {
  implicit override def F: CoFlatMap[F]

  def coFlatMapAssociativity[A, B, C](fa: F[A], f: F[A] => B, g: F[B] => C): IsEq[F[C]] =
    fa.coflatMap(f).coflatMap(g) <-> fa.coflatMap(x => g(x.coflatMap(f)))

  /**
   * The composition of [[cats.data.Cokleisli]] arrows is associative. This is
   * analogous to the associativity law of [[CoFlatMap.coflatMap]].
   */
  def cokleisliAssociativity[A, B, C, D](f: F[A] => B, g: F[B] => C, h: F[C] => D, fa: F[A]): IsEq[D] = {
    val (cf, cg, ch) = (Cokleisli(f), Cokleisli(g), Cokleisli(h))
    (cf compose (cg compose ch)).run(fa) <-> ((cf compose cg) compose ch).run(fa)
  }
}

object CoFlatMapLaws {
  def apply[F[_]](implicit ev: CoFlatMap[F]): CoFlatMapLaws[F] =
    new CoFlatMapLaws[F] { def F = ev }
}
