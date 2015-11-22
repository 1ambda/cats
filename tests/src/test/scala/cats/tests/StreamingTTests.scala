package cats
package tests

import algebra.laws.OrderLaws

import cats.data.StreamingT
import cats.laws.discipline.{MonoidalTests, CoflatMapTests, MonadCombineTests, SerializableTests}
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._

class StreamingTTests extends CatsSuite {

  {
    implicit val iso = MonoidalTests.Isomorphisms.invariant[StreamingT[Eval, ?]]
    checkAll("StreamingT[Eval, ?]", MonadCombineTests[StreamingT[Eval, ?]].monad[Int, Int, Int])
    checkAll("StreamingT[Eval, ?]", CoflatMapTests[StreamingT[Eval, ?]].coflatMap[Int, Int, Int])
    checkAll("StreamingT[Eval, Int]", OrderLaws[StreamingT[Eval, Int]].order)
    checkAll("Monad[StreamingT[Eval, ?]]", SerializableTests.serializable(Monad[StreamingT[Eval, ?]]))
  }

  {
    implicit val iso = MonoidalTests.Isomorphisms.invariant[StreamingT[Option, ?]]
    checkAll("StreamingT[Option, ?]", MonadCombineTests[StreamingT[Option, ?]].monad[Int, Int, Int])
    checkAll("StreamingT[Option, ?]", CoflatMapTests[StreamingT[Option, ?]].coflatMap[Int, Int, Int])
    checkAll("StreamingT[Option, Int]", OrderLaws[StreamingT[Option, Int]].order)
    checkAll("Monad[StreamingT[Option, ?]]", SerializableTests.serializable(Monad[StreamingT[Option, ?]]))
  }

  {
    implicit val iso = MonoidalTests.Isomorphisms.invariant[StreamingT[List, ?]]
    checkAll("StreamingT[List, ?]", MonadCombineTests[StreamingT[List, ?]].monad[Int, Int, Int])
    checkAll("StreamingT[List, ?]", CoflatMapTests[StreamingT[List, ?]].coflatMap[Int, Int, Int])
    checkAll("StreamingT[List, Int]", OrderLaws[StreamingT[List, Int]].order)
    checkAll("Monad[StreamingT[List, ?]]", SerializableTests.serializable(Monad[StreamingT[List, ?]]))
  }

  test("uncons with Id consistent with List headOption/tail") {
    forAll { (s: StreamingT[Id, Int]) =>
      val sList = s.toList
      s.uncons.map{ case (h, t) =>
        (h, t.toList)
      } should === (sList.headOption.map{ h =>
        (h, sList.tail)
      })
    }
  }

  test("map with Id consistent with List.map") {
    forAll { (s: StreamingT[Id, Int], f: Int => Long) =>
      s.map(f).toList should === (s.toList.map(f))
    }
  }

  test("flatMap with Id consistent with List.flatMap") {
    forAll { (s: StreamingT[Id, Int], f: Int => StreamingT[Id, Long]) =>
      s.flatMap(f).toList should === (s.toList.flatMap(f(_).toList))
    }
  }

  test("filter with Id consistent with List.filter") {
    forAll { (s: StreamingT[Id, Int], f: Int => Boolean) =>
      s.filter(f).toList should === (s.toList.filter(f))
    }
  }

  test("foldLeft with Id consistent with List.foldLeft") {
    forAll { (s: StreamingT[Id, Int], l: Long, f: (Long, Int) => Long) =>
      s.foldLeft(l)(f) should === (s.toList.foldLeft(l)(f))
    }
  }

  test("find with Id consistent with List.find") {
    forAll { (s: StreamingT[Id, Int], f: Int => Boolean) =>
      s.find(f) should === (s.toList.find(f))
    }
  }

  test("isEmpty with Id consistent with List.isEmpty") {
    forAll { (s: StreamingT[Id, Int]) =>
      s.isEmpty should === (s.toList.isEmpty)
    }
  }

  test("nonEmpty with Id consistent with List.nonEmpty") {
    forAll { (s: StreamingT[Id, Int]) =>
      s.nonEmpty should === (s.toList.nonEmpty)
    }
  }

  test("%:: with Id consistent with List.::") {
    forAll { (i: Int, s: StreamingT[Id, Int]) =>
      (i %:: s).toList should === (i :: s.toList)
    }
  }

  test("%::: with Id consistent with List.:::") {
    forAll { (s1: StreamingT[Id, Int], s2: StreamingT[Id, Int]) =>
      (s1 %::: s2).toList should === (s1.toList ::: s2.toList)
    }
  }

  test("concat with Id consistent with List.++") {
    forAll { (s1: StreamingT[Id, Int], s2: StreamingT[Id, Int]) =>
      (s1 concat s2).toList should === (s1.toList ++ s2.toList)
    }
  }

  test("exists with Id consistent with List.exists") {
    forAll { (s: StreamingT[Id, Int], f: Int => Boolean) =>
      s.exists(f) should === (s.toList.exists(f))
    }
  }

  test("forall with Id consistent with List.forall") {
    forAll { (s: StreamingT[Id, Int], f: Int => Boolean) =>
      s.forall(f) should === (s.toList.forall(f))
    }
  }

  test("takeWhile with Id consistent with List.takeWhile") {
    forAll { (s: StreamingT[Id, Int], f: Int => Boolean) =>
      s.takeWhile(f).toList should === (s.toList.takeWhile(f))
    }
  }

  test("dropWhile with Id consistent with List.dropWhile") {
    forAll { (s: StreamingT[Id, Int], f: Int => Boolean) =>
      s.dropWhile(f).toList should === (s.toList.dropWhile(f))
    }
  }

  test("take with Id consistent with List.take") {
    forAll { (s: StreamingT[Id, Int], i: Int) =>
      s.take(i).toList should === (s.toList.take(i))
    }
  }

  test("drop with Id consistent with List.drop") {
    forAll { (s: StreamingT[Id, Int], i: Int) =>
      s.drop(i).toList should === (s.toList.drop(i))
    }
  }
}

class SpecificStreamingTTests extends CatsSuite {

  type S[A] = StreamingT[List, A]

  def cons[A](a: A, fs: List[S[A]]): S[A] = StreamingT.cons(a, fs)
  def wait[A](fs: List[S[A]]): S[A] = StreamingT.wait(fs)
  def empty[A]: S[A] = StreamingT.empty[List, A]

  test("counter-example #1"){
    val fa: S[Boolean] =
      cons(true, List(cons(true, List(empty)), empty))

    def f(b: Boolean): S[Boolean] =
      if (b) cons(false, List(cons(true, List(empty))))
      else empty

    def g(b: Boolean): S[Boolean] =
      if (b) empty
      else cons(true, List(cons(false, List(empty)), cons(true, List(empty))))

    val x = fa.flatMap(f).flatMap(g)
    val y = fa.flatMap(a => f(a).flatMap(g))
    x should === (y)
  }
}
