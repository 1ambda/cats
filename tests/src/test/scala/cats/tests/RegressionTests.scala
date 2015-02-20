package cats.tests

import cats._
import scala.collection.mutable

class RegressionTests extends CatsSuite {

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

  // used to test side-effects
  val buf = mutable.ListBuffer.empty[String]

  case class Person(id: Int, name: String)

  def alloc(name: String): State[Int, Person] =
    State { id =>
      buf.append(name)
      (Person(id, name), id + 1)
    }

  test("#140: confirm sequence order") {

    // test result order
    val ons = List(Option(1), Option(2), Option(3))
    assert(Traverse[List].sequence(ons) == Some(List(1, 2, 3)))

    // test order of effects using a contrived, unsafe state monad.
    val names = List("Alice", "Bob", "Claire")
    val allocated = names.map(alloc)
    val state = Traverse[List].sequence[State[Int, ?],Person](allocated)
    val (people, counter) = state.run(0)
    assert(people == List(Person(0, "Alice"), Person(1, "Bob"), Person(2, "Claire")))
    assert(counter == 3)

    // ensure that side-effects occurred in "correct" order
    assert(buf.toList == names)
  }
}
