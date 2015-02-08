package cats

import simulacrum._

/**
 * Data structures that can be folded to a summary value.
 *
 * See: [[https://www.cs.nott.ac.uk/~gmh/fold.pdf A tutorial on the universality and expressiveness of fold]]
 */
@typeclass trait Foldable[F[_]] { self =>

  /**
   * Left associative fold on 'F' using the function 'f'.
   */
  def foldLeft[A, B](fa: F[A], b: B)(f: (B, A) => B): B

  /**
   * Right associative fold on 'F' using the function 'f'.
   */
  def foldRight[A, B](fa: F[A], b: B)(f: (A, B) => B): B

  /**
   * Left for issue #62
   */
  def foldRight[A, B](fa: F[A], b: Lazy[B])(f: (A, Lazy[B]) => B): Lazy[B]

  /**
   * Apply f to each element of F and combine them using the Monoid[B].
   */
  def foldMap[A, B: Monoid](fa: F[A])(f: A => B): B = foldLeft(fa, Monoid[B].empty) { (b, a) =>
    Monoid[B].combine(b, f(a))
  }

  /**
   * Fold up F using the Monoid[A]
   */
  def fold[A: Monoid](fa: F[A]): A = foldMap(fa)(x => x)

  /**
   * Traverse F in the Applicative G and ignore the return values of 'f'.
   */
  def traverse_[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[Unit] =
    foldLeft(fa, Applicative[G].pure(())) { (acc, a) =>
      Applicative[G].map2(acc, f(a)) { (_, _) => () }
    }

  /**
   * Traverse F in the Applicative G ignoring all values in fga.
   */
  def sequence_[G[_]: Applicative, A, B](fga: F[G[A]]): G[Unit] = traverse_(fga)(identity)

  /**
   * Fold up F using the MonoidK instance for G. Like fold, but the value is of kind * -> *.
   */
  def foldK[G[_]: MonoidK, A](fga: F[G[A]]): G[A] = foldMap(fga)(identity)(MonoidK[G].algebra)

  /**
   * Compose this foldable instance with one for G creating Foldable[F[G]]
   */
  def compose[G[_]](implicit GG: Foldable[G]): Foldable[λ[α => F[G[α]]]] =
    new CompositeFoldable[F, G] {
      implicit def F: Foldable[F] = self
      implicit def G: Foldable[G] = GG
    }
}

/**
 * Methods that apply to 2 nested Foldable instances
 */
trait CompositeFoldable[F[_], G[_]] extends Foldable[λ[α => F[G[α]]]] {
  implicit def F: Foldable[F]
  implicit def G: Foldable[G]

  /**
   * Left assocative fold on F[G[A]] using 'f'
   */
  def foldLeft[A, B](fa: F[G[A]], b: B)(f: (B, A) => B): B =
    F.foldLeft(fa, b)((b, a) => G.foldLeft(a, b)(f))

  /**
   * Left assocative fold on F[G[A]] using 'f'
   */
  def foldRight[A, B](fa: F[G[A]], b: B)(f: (A, B) => B): B =
    F.foldRight(fa, b)((a, b) => G.foldRight(a, b)(f))

  /**
   * Left for issue #62
   */
  def foldRight[A, B](fa: F[G[A]], b: Lazy[B])(f: (A, Lazy[B]) => B): Lazy[B] =
    F.foldRight(fa, b)((a, b) => G.foldRight(a, b)(f).force)
}
