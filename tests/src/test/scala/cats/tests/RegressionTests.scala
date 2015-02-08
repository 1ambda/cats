package cats.tests

import cats._
import cats.implicits._
import org.scalatest.FunSuite

class RegressionTests extends FunSuite {

  // test sequence order using a contrived, unsafe state monad.
  // 

  test("#140: confirm sequence order") {
    val allocated = List("Alice", "Bob", "Claire").map(alloc)
    val state = Traverse[List].sequence[State[Int, ?],Person](allocated)
    val (people, counter) = state.run(0)
    assert(people == List(Person(0, "Alice"), Person(1, "Bob"), Person(2, "Claire")))
    assert(counter == 3)
  }

  case class Person(id: Int, name: String)

  def alloc(name: String): State[Int, Person] =
    State(id => (Person(id, name), id + 1))

  // toy state class
  // not stack safe, very minimal, not for actual use

  case class State[S, A](run: S => (A, S)) { self =>
    def map[B](f: A => B) =
      State[S, B]({ s => val (a, s2) = self.run(s); (f(a), s2) })
    def flatMap[B](f: A => State[S, B]) =
      State[S, B]({ s => val (a, s2) = self.run(s); f(a).run(s2) })
  }

  object State {
    implicit def instance[S] = new Monad[State[S, ?]] {
      def pure[A](a: A) = State[S, A](s => (a, s))
      def flatMap[A, B](sa: State[S, A])(f: A => State[S, B]) = sa.flatMap(f)
    }
  }
}
