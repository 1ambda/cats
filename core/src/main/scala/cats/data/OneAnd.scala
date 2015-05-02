package cats
package data

/**
 * A data type which represents a single element (head) and some other
 * structure (tail). As we have done in package.scala, this can be
 * used to represent a List which is guaranteed to not be empty:
 *
 * {{{
 * type NonEmptyList[A] = OneAnd[A, List]
 * }}}
 */
final case class OneAnd[A, F[_]](head: A, tail: F[A]) {

  /**
   * Combine the head and tail into a single `F[A]` value.
   */
  def unwrap(implicit F: MonadCombine[F]): F[A] =
    F.combine(F.pure(head), tail)

  /**
   * remove elements not matching the predicate
   */
  def filter(f: A => Boolean)(implicit F: MonadCombine[F]): F[A] = {
    val rest = F.filter(tail)(f)
    if (f(head)) F.combine(F.pure(head), rest) else rest
  }

  /**
   * Append another OneAnd to this
   */
  def combine(other: OneAnd[A, F])(implicit F: MonadCombine[F]): OneAnd[A, F] =
    OneAnd(head, F.combine(tail, F.combine(F.pure(other.head), other.tail)))

  /**
   * find the first element matching the predicate, if one exists
   */
  def find(f: A => Boolean)(implicit F: Foldable[F]): Option[A] =
    if (f(head)) Some(head) else F.find(tail)(f)

  /**
   * Left-associative fold on the structure using f.
   */
  def foldLeft[B](b: B)(f: (B, A) => B)(implicit F: Foldable[F]): B =
    F.foldLeft(tail, f(b, head))(f)

  /**
   * Right-associative fold on the structure using f.
   */
  def foldRight[B](b: Lazy[B])(f: A => Fold[B])(implicit F: Foldable[F]): Lazy[B] =
    Lazy(f(head).complete(F.foldRight(tail, b)(f)))

  /**
   * Typesafe equality operator.
   *
   * This method is similar to == except that it only allows two
   * OneAnd[A, F] values to be compared to each other, and uses
   * equality provided by Eq[_] instances, rather than using the
   * universal equality provided by .equals.
   */
  def ===(that: OneAnd[A, F])(implicit A: Eq[A], FA: Eq[F[A]]): Boolean =
    A.eqv(head, that.head) && FA.eqv(tail, that.tail)

  /**
   * Typesafe stringification method.
   *
   * This method is similar to .toString except that it stringifies
   * values according to Show[_] instances, rather than using the
   * universal .toString method.
   */
  def show(implicit A: Show[A], FA: Show[F[A]]): String =
    s"OneAnd(${A.show(head)}, ${FA.show(tail)})"
}

trait OneAndInstances {

  implicit def oneAndEq[A, F[_]](implicit A: Eq[A], FA: Eq[F[A]]): Eq[OneAnd[A, F]] =
    new Eq[OneAnd[A, F]]{
      def eqv(x: OneAnd[A, F], y: OneAnd[A, F]): Boolean = x === y
    }

  implicit def oneAndShow[A, F[_]](implicit A: Show[A], FA: Show[F[A]]): Show[OneAnd[A, F]] =
    Show.show[OneAnd[A, F]](_.show)

  implicit def oneAndFunctor[F[_]](F: Functor[F]): Functor[OneAnd[?, F]] =
    new Functor[OneAnd[?, F]] {
      def map[A, B](fa: OneAnd[A, F])(f: A => B) =
        OneAnd(f(fa.head), F.map(fa.tail)(f))
    }

  implicit def oneAndSemigroupK[F[_]: MonadCombine]: SemigroupK[OneAnd[?, F]] =
    new SemigroupK[OneAnd[?, F]] {
      def combine[A](a: OneAnd[A, F], b: OneAnd[A, F]) = a combine b
    }

  implicit def oneAndReducible[F[_]](implicit F: Foldable[F]): Reducible[OneAnd[?, F]] =
    new NonEmptyReducible[OneAnd[?, F], F] {
      def split[A](fa: OneAnd[A, F]): (A, F[A]) =
        (fa.head, fa.tail)
    }

  implicit def oneAndMonad[F[_]](implicit F: MonadCombine[F]): Monad[OneAnd[?, F]] =
    new Monad[OneAnd[?, F]] {

      override def map[A, B](fa: OneAnd[A, F])(f: A => B): OneAnd[B, F] =
        OneAnd(f(fa.head), F.map(fa.tail)(f))

      def pure[A](x: A): OneAnd[A, F] =
        OneAnd(x, F.empty)

      def flatMap[A, B](fa: OneAnd[A, F])(f: A => OneAnd[B, F]): OneAnd[B, F] = {
        val end = F.flatMap(fa.tail) { a =>
          val fa = f(a)
          F.combine(F.pure(fa.head), fa.tail)
        }
        val fst = f(fa.head)
        OneAnd(fst.head, F.combine(fst.tail, end))
      }
    }
}

object OneAnd extends OneAndInstances
