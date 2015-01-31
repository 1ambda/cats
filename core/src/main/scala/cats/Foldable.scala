package cats

import algebra.Monoid
import simulacrum._

@typeclass trait Foldable[F[_]] { self =>
  def foldLeft[A, B](fa: F[A], b: B)(f: (B, A) => B): B

  def foldRight[A, B](fa: F[A], b: B)(f: (A, B) => B): B

  def foldRight[A, B](fa: F[A], b: Lazy[B])(f: (A, Lazy[B]) => B): Lazy[B]

  def foldMap[A, B: Monoid](fa: F[A])(f: A => B): B =
    foldLeft(fa, Monoid[B].empty) { (b, a) =>
      Monoid[B].combine(b, f(a))
    }

  def fold[A: Monoid](fa: F[A]): A =
    foldMap(fa)(x => x)

  def traverse_[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[Unit] =
    foldLeft(fa, Applicative[G].pure(())) { (acc, a) =>
      Applicative[G].map2(acc, f(a)) { (_, _) => () }
    }

  def sequence_[G[_]: Applicative, A, B](fga: F[G[A]]): G[Unit] =
    traverse_(fga)(identity)

  def psum[G[_]: MonoidK, A](fga: F[G[A]]): G[A] =
    foldLeft(fga, MonoidK[G].empty[A])(MonoidK[G].combine)

  def compose[G[_]](implicit GG: Foldable[G]): Foldable[λ[α => F[G[α]]]] =
    new CompositeFoldable[F,G] {
      implicit def F: Foldable[F] = self
      implicit def G: Foldable[G] = GG
    }
}

trait CompositeFoldable[F[_],G[_]] extends Foldable[λ[α => F[G[α]]]] {
  implicit def F: Foldable[F]
  implicit def G: Foldable[G]

  def foldLeft[A, B](fa: F[G[A]], b: B)(f: (B, A) => B): B =
    F.foldLeft(fa, b)((b, a) => G.foldLeft(a, b)(f))

  def foldRight[A, B](fa: F[G[A]], b: B)(f: (A, B) => B): B =
    F.foldRight(fa, b)((a, b) => G.foldRight(a,b)(f))

  def foldRight[A, B](fa: F[G[A]], b: Lazy[B])(f: (A, Lazy[B]) => B): Lazy[B] =
    F.foldRight(fa, b)((a, b) => G.foldRight(a,b)(f).force)
}
