package cats
package state

import cats.tests.CatsSuite
import cats.free.FreeTests._
import cats.laws.discipline.{MonadStateTests, MonoidKTests, SerializableTests}
import cats.laws.discipline.eq._
import org.scalacheck.{Arbitrary, Gen}

class StateTTests extends CatsSuite {
  import StateTTests._

  test("basic state usage"){
    add1.run(1).run should === (2 -> 1)
  }

  test("traversing state is stack-safe"){
    val ns = (0 to 100000).toList
    val x = ns.traverseU(_ => add1)
    x.runS(0).run should === (100001)
  }

  test("State.pure and StateT.pure are consistent"){
    forAll { (s: String, i: Int) =>
      val state: State[String, Int] = State.pure(i)
      val stateT: State[String, Int] = StateT.pure(i)
      state.run(s).run should === (stateT.run(s).run)
    }
  }

  test("Apply syntax is usable on State") {
    val x = add1 *> add1
    x.runS(0).run should === (2)
  }

  test("Singleton and instance inspect are consistent"){
    forAll { (s: String, i: Int) =>
      State.inspect[Int, String](_.toString).run(i).run should === (
        State.pure[Int, Unit](()).inspect(_.toString).run(i).run)
    }
  }

  test("runEmpty, runEmptyS, and runEmptyA consistent"){
    forAll { (f: StateT[List, Long, Int]) =>
      (f.runEmptyS zip f.runEmptyA) should === (f.runEmpty)
    }
  }

  test("modify identity is a noop"){
    forAll { (f: StateT[List, Long, Int]) =>
      f.modify(identity) should === (f)
    }
  }

  test("modify modifies state"){
    forAll { (f: StateT[List, Long, Int], g: Long => Long, initial: Long) =>
      f.modify(g).runS(initial) should === (f.runS(initial).map(g))
    }
  }

  test("modify doesn't affect A value"){
    forAll { (f: StateT[List, Long, Int], g: Long => Long, initial: Long) =>
      f.modify(g).runA(initial) should === (f.runA(initial))
    }
  }

  test("State.modify equivalent to get then set"){
    forAll { (f: Long => Long) =>
      val s1 = for {
        l <- State.get[Long]
        _ <- State.set(f(l))
      } yield ()

      val s2 = State.modify(f)

      s1 should === (s2)
    }
  }

  checkAll("StateT[Option, Int, Int]", MonadStateTests[StateT[Option, Int, ?], Int].monadState[Int, Int, Int])
  checkAll("MonadState[StateT[Option, ?, ?], Int]", SerializableTests.serializable(MonadState[StateT[Option, Int, ?], Int]))

  checkAll("State[Long, ?]", MonadStateTests[State[Long, ?], Long].monadState[Int, Int, Int])
  checkAll("MonadState[State[Long, ?], Long]", SerializableTests.serializable(MonadState[State[Long, ?], Long]))
}

object StateTTests extends StateTTestsInstances {
  implicit def stateEq[S:Eq:Arbitrary, A:Eq]: Eq[State[S, A]] =
    stateTEq[free.Trampoline, S, A]

  implicit def stateArbitrary[S: Arbitrary, A: Arbitrary]: Arbitrary[State[S, A]] =
    stateTArbitrary[free.Trampoline, S, A]

  val add1: State[Int, Int] = State(n => (n + 1, n))
}

sealed trait StateTTestsInstances {
  implicit def stateTArbitrary[F[_]: Applicative, S, A](implicit F: Arbitrary[S => F[(S, A)]]): Arbitrary[StateT[F, S, A]] =
    Arbitrary(F.arbitrary.map(f => StateT(f)))

  implicit def stateTEq[F[_], S, A](implicit S: Arbitrary[S], FSA: Eq[F[(S, A)]], F: FlatMap[F]): Eq[StateT[F, S, A]] =
    Eq.by[StateT[F, S, A], S => F[(S, A)]](state =>
      s => state.run(s))
}
