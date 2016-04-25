package cats.kernel
package std

import cats.kernel.std.util.StaticMethods

package object stream extends StreamInstances

trait StreamInstances extends StreamInstances1 {
  implicit def streamOrder[A: Order] = new StreamOrder[A]
  implicit def streamMonoid[A] = new StreamMonoid[A]
}

trait StreamInstances1 extends StreamInstances2 {
  implicit def streamPartialOrder[A: PartialOrder] = new StreamPartialOrder[A]
}

trait StreamInstances2 {
  implicit def streamEq[A: Eq] = new StreamEq[A]
}

class StreamOrder[A](implicit ev: Order[A]) extends Order[Stream[A]] {
  def compare(xs: Stream[A], ys: Stream[A]): Int =
    StaticMethods.iteratorCompare(xs.iterator, ys.iterator)
}

class StreamPartialOrder[A](implicit ev: PartialOrder[A]) extends PartialOrder[Stream[A]] {
  def partialCompare(xs: Stream[A], ys: Stream[A]): Double =
    StaticMethods.iteratorPartialCompare(xs.iterator, ys.iterator)
}

class StreamEq[A](implicit ev: Eq[A]) extends Eq[Stream[A]] {
  def eqv(xs: Stream[A], ys: Stream[A]): Boolean =
    StaticMethods.iteratorEq(xs.iterator, ys.iterator)
}

class StreamMonoid[A] extends Monoid[Stream[A]] {
  def empty: Stream[A] = Stream.empty
  def combine(x: Stream[A], y: Stream[A]): Stream[A] = x ++ y

  override def combineN(x: Stream[A], n: Int): Stream[A] = {
    val buf = Stream.newBuilder[A]
    var i = n
    while (i > 0) {
      buf ++= x
      i -= 1
    }
    buf.result
  }

  override def combineAll(xs: TraversableOnce[Stream[A]]): Stream[A] = {
    val buf = Stream.newBuilder[A]
    xs.foreach(buf ++= _)
    buf.result
  }
}
