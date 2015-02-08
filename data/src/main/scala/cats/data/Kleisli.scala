package cats.data

import cats._
import cats.functor.Strong

/**
 * Represents a function `A => F[B]`.
 */
final case class Kleisli[F[_], A, B](runKleisli: A => F[B]) { self =>

  def apply[C](f: Kleisli[F, A, B => C])(implicit F: Apply[F]): Kleisli[F, A, C] =
    Kleisli(a => F.apply(runKleisli(a))(f.runKleisli(a)))

  def dimap[C, D](f: C => A)(g: B => D)(implicit F: Functor[F]): Kleisli[F, C, D] =
    Kleisli(c => F.map(runKleisli(f(c)))(g))

  def lmap[C](f: C => A): Kleisli[F, C, B] =
    Kleisli(runKleisli compose f)

  def map[C](f: B => C)(implicit M: Functor[F]): Kleisli[F, A, C] =
    Kleisli(a => M.map(runKleisli(a))(f))

  def mapK[N[_], C](f: F[B] => N[C]): Kleisli[N, A, C] =
    Kleisli(runKleisli andThen f)

  def flatMap[C](f: B => F[C])(implicit M: FlatMap[F]): Kleisli[F, A, C] =
    Kleisli(a => M.flatMap(runKleisli(a))(f))

  def flatMapK[C](f: B => Kleisli[F, A, C])(implicit M: FlatMap[F]): Kleisli[F, A, C] =
    Kleisli((r: A) => M.flatMap[B, C](runKleisli(r))((b: B) => f(b).runKleisli(r)))

  def andThen[C](f: B => F[C])(implicit b: FlatMap[F]): Kleisli[F, A, C] =
    Kleisli((a: A) => b.flatMap(runKleisli(a))(f))

  def andThen[C](k: Kleisli[F, B, C])(implicit b: FlatMap[F]): Kleisli[F, A, C] =
    this andThen k.runKleisli

  def compose[Z](f: Z => F[A])(implicit M: FlatMap[F]): Kleisli[F, Z, B] =
    Kleisli((z: Z) => M.flatMap(f(z))(runKleisli))

  def compose[Z](k: Kleisli[F, Z, A])(implicit b: FlatMap[F]): Kleisli[F, Z, B] =
    this compose k.runKleisli

  def traverse[G[_]](f: G[A])(implicit M: Applicative[F], F: Traverse[G]): F[G[B]] =
    F.traverse(f)(runKleisli)

  def lift[G[_]](implicit F: Applicative[F]): Kleisli[λ[α => F[F[α]]], A, B] =
    Kleisli[λ[α => F[F[α]]], A, B](a => Applicative[F].pure(runKleisli(a)))

  def lower(implicit M: Monad[F]): Kleisli[F, A, F[B]] =
    Kleisli(a => M.pure(runKleisli(a)))

  def first[C](implicit F: Functor[F]): Kleisli[F, (A, C), (B, C)] =
    Kleisli{ case (a, c) => F.fproduct(runKleisli(a))(_ => c)}

  def second[C](implicit F: Functor[F]): Kleisli[F, (C, A), (C, B)] =
    Kleisli{ case (c, a) => F.map(runKleisli(a))(c -> _)}
}

object Kleisli extends KleisliInstances with KleisliFunctions

sealed trait KleisliFunctions {
  /** creates a [[Kleisli]] from a [[Function1]] */
  def kleisli[F[_], A, B](f: A => F[B]): Kleisli[F, A, B] =
    Kleisli(f)

  def pure[F[_], A, B](x: B)(implicit F: Applicative[F]): Kleisli[F, A, B] =
    Kleisli(_ => F.pure(x))

  def ask[F[_], A](implicit F: Applicative[F]): Kleisli[F, A, A] =
    Kleisli(F.pure)

  def local[M[_], A, R](f: R => R)(fa: Kleisli[M, R, A]): Kleisli[M, R, A] =
    Kleisli(f andThen fa.runKleisli)
}

sealed abstract class KleisliInstances extends KleisliInstances0 {
  implicit def upStarStrong[F[_]: Functor]: Strong[Kleisli[F, ?, ?]] = new Strong[Kleisli[F, ?, ?]]{
    override def lmap[A, B, C](fab: Kleisli[F, A, B])(f: C => A): Kleisli[F, C, B] =
      fab.lmap(f)

    override def rmap[A, B, C](fab: Kleisli[F, A, B])(f: B => C): Kleisli[F, A, C] =
      fab.map(f)

    override def dimap[A, B, C, D](fab: Kleisli[F, A, B])(f: C => A)(g: B => D): Kleisli[F, C, D] =
      fab.dimap(f)(g)

    def first[A, B, C](fa: Kleisli[F, A, B]): Kleisli[F, (A, C), (B, C)] =
      fa.first[C]

    def second[A, B, C](fa: Kleisli[F, A, B]): Kleisli[F, (C, A), (C, B)] =
      fa.second[C]
  }

  implicit def upStarMonad[F[_]: Monad, A]: Monad[Kleisli[F, A, ?]] = new Monad[Kleisli[F, A, ?]] {
    def pure[B](x: B): Kleisli[F, A, B] =
      Kleisli.pure[F, A, B](x)

    def flatMap[B, C](fa: Kleisli[F, A, B])(f: B => Kleisli[F, A, C]): Kleisli[F, A, C] =
      fa.flatMapK(f)
  }
}

sealed abstract class KleisliInstances0 extends KleisliInstances1 {
  implicit def upStarFlatMap[F[_]: FlatMap, A]: FlatMap[Kleisli[F, A, ?]] = new FlatMap[Kleisli[F, A, ?]] {
    def flatMap[B, C](fa: Kleisli[F, A, B])(f: B => Kleisli[F, A, C]): Kleisli[F, A, C] =
      fa.flatMapK(f)

    def map[B, C](fa: Kleisli[F, A, B])(f: B => C): Kleisli[F, A, C] =
      fa.map(f)
  }
}

sealed abstract class KleisliInstances1 extends KleisliInstances2 {
  implicit def upStarApplicative[F[_]: Applicative, A]: Applicative[Kleisli[F, A, ?]] = new Applicative[Kleisli[F, A, ?]] {
    def pure[B](x: B): Kleisli[F, A, B] =
      Kleisli.pure[F, A, B](x)

    def apply[B, C](fa: Kleisli[F, A, B])(f: Kleisli[F, A, B => C]): Kleisli[F, A, C] =
      fa.apply(f)
  }
}

sealed abstract class KleisliInstances2 extends KleisliInstances3 {
  implicit def upStarApply[F[_]: Apply, A]: Apply[Kleisli[F, A, ?]] = new Apply[Kleisli[F, A, ?]] {
    def apply[B, C](fa: Kleisli[F, A, B])(f: Kleisli[F, A, B => C]): Kleisli[F, A, C] =
      fa.apply(f)

    def map[B, C](fa: Kleisli[F, A, B])(f: B => C): Kleisli[F, A, C] =
      fa.map(f)
  }
}

sealed abstract class KleisliInstances3 {
  implicit def upStarFunctor[F[_]: Functor, A]: Functor[Kleisli[F, A, ?]] = new Functor[Kleisli[F, A, ?]] {
    def map[B, C](fa: Kleisli[F, A, B])(f: B => C): Kleisli[F, A, C] =
      fa.map(f)
  }
}