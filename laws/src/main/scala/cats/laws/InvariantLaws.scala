package cats.laws

import cats.functor.Invariant
import cats.syntax.invariant._

/**
 * Laws that must be obeyed by any [[cats.functor.Invariant]].
 */
class InvariantLaws[F[_]](implicit F: Invariant[F]) {
  def invariantIdentity[A](fa: F[A]): (F[A], F[A]) =
    fa.imap(identity[A])(identity[A]) -> fa

  def invariantComposition[A, B, C](fa: F[A], f1: A => B, f2: B => A, g1: B => C, g2: C => B): (F[C], F[C]) =
    fa.imap(f1)(f2).imap(g1)(g2) -> fa.imap(f1 andThen g1)(g2 andThen f2)

}
