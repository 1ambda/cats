package cats
package free

import cats.arrow.NaturalTransformation
import cats.tests.CatsSuite

class FreeApplicativeTests extends CatsSuite {
  test("FreeApplicative#compile") {
    val x = FreeApplicative.lift[Id, Int](1)
    val y = FreeApplicative.pure[Id, Int](2)
    val f = x.map(i => (j: Int) => i + j)
    val nt = NaturalTransformation.id[Id]
    val r1 = x.ap(f)
    val r2 = r1.compile(nt)
    assert(r1.foldMap(nt) == r2.foldMap(nt))
  }

  test("FreeApplicative#monad") {
    val x = FreeApplicative.lift[Id, Int](1)
    val y = FreeApplicative.pure[Id, Int](2)
    val f = x.map(i => (j: Int) => i + j)
    val r1 = y.ap(f)
    val r2 = r1.monad
    val nt =
      new NaturalTransformation[Id, Id] {
        def apply[A](fa: Id[A]): Id[A] = fa
      }
    assert(r1.foldMap(nt) == r2.foldMap(nt))
  }
}
