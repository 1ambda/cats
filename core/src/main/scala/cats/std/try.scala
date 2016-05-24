package cats
package std

import cats.syntax.all._
import cats.data.Xor

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait TryInstances extends TryInstances1 {

  implicit def tryInstance: MonadError[Try, Throwable] with CoflatMap[Try] =
    new TryCoflatMap with MonadError[Try, Throwable]{
      def pure[A](x: A): Try[A] = Success(x)

      override def pureEval[A](x: Eval[A]): Try[A] = x match {
        case Now(x) => Success(x)
        case _ => Try(x.value)
      }

      override def map2[A, B, Z](ta: Try[A], tb: Try[B])(f: (A, B) => Z): Try[Z] =
        ta.flatMap(a => tb.map(b => f(a, b)))

      override def map2Eval[A, B, Z](ta: Try[A], tb: Eval[Try[B]])(f: (A, B) => Z): Eval[Try[Z]] =
        ta match {
          case f@Failure(_) => Now(f.asInstanceOf[Try[Z]])
          case Success(a) => tb.map(_.map(f(a, _)))
        }

      def flatMap[A, B](ta: Try[A])(f: A => Try[B]): Try[B] = ta.flatMap(f)

      def handleErrorWith[A](ta: Try[A])(f: Throwable => Try[A]): Try[A] =
        ta.recoverWith { case t => f(t) }

      def raiseError[A](e: Throwable): Try[A] = Failure(e)
      override def handleError[A](ta: Try[A])(f: Throwable => A): Try[A] =
        ta.recover { case t => f(t) }

      override def attempt[A](ta: Try[A]): Try[Throwable Xor A] =
        (ta map Xor.right) recover { case NonFatal(t) => Xor.left(t) }

      override def recover[A](ta: Try[A])(pf: PartialFunction[Throwable, A]): Try[A] =
        ta.recover(pf)

      override def recoverWith[A](ta: Try[A])(pf: PartialFunction[Throwable, Try[A]]): Try[A] = ta.recoverWith(pf)

      override def map[A, B](ta: Try[A])(f: A => B): Try[B] = ta.map(f)
    }

  implicit def tryGroup[A: Group]: Group[Try[A]] =
    new TryGroup[A]

  implicit def showTry[A](implicit A: Show[A]): Show[Try[A]] =
    new Show[Try[A]] {
      def show(fa: Try[A]): String = fa match {
        case Success(a) => s"Success(${A.show(a)})"
        case Failure(e) => s"Failure($e)"
      }
    }
  implicit def eqTry[A](implicit A: Eq[A]): Eq[Try[A]] =
    new Eq[Try[A]] {
      def eqv(x: Try[A], y: Try[A]): Boolean = (x, y) match {
        case (Success(a), Success(b)) => A.eqv(a, b)
        case (Failure(_), Failure(_)) => true // all failures are equivalent
        case _ => false
      }
    }
}

private[std] sealed trait TryInstances1 extends TryInstances2 {
  implicit def tryMonoid[A: Monoid]: Monoid[Try[A]] =
    new TryMonoid[A]
}

private[std] sealed trait TryInstances2 {
  implicit def trySemigroup[A: Semigroup]: Semigroup[Try[A]] =
    new TrySemigroup[A]
}

private[cats] abstract class TryCoflatMap extends CoflatMap[Try] {
  def map[A, B](ta: Try[A])(f: A => B): Try[B] = ta.map(f)
  def coflatMap[A, B](ta: Try[A])(f: Try[A] => B): Try[B] = Try(f(ta))
}

private[cats] class TrySemigroup[A: Semigroup] extends Semigroup[Try[A]] {
  def combine(fx: Try[A], fy: Try[A]): Try[A] =
    for {
      x <- fx
      y <- fy
    } yield x |+| y
}

private[cats] class TryMonoid[A](implicit A: Monoid[A]) extends TrySemigroup[A] with Monoid[Try[A]] {
  def empty: Try[A] = Success(A.empty)
}

private[cats] class TryGroup[A](implicit A: Group[A]) extends TryMonoid[A] with Group[Try[A]] {
  def inverse(fx: Try[A]): Try[A] =
    fx.map(_.inverse)
  override def remove(fx: Try[A], fy: Try[A]): Try[A] =
    for {
      x <- fx
      y <- fy
    } yield x |-| y
}
