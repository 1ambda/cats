---
layout: default
title:  "FAQ"
section: "faq"
---

# Frequently Asked Questions

## Questions

 * [What imports do I need?](#what-imports)
 * [Why can't the compiler find implicit instances for Future?](#future-instances)
 * [How can I turn my List of `<something>` into a `<something>` of a list?](#traverse)
 * [What does `@typeclass` mean?](#simulacrum)
 * [What do types like `?` and `λ` mean?](#kind-projector)
 * [What does `macro Ops` do? What is `cats.macros.Ops`?](#machinist)
 * [How can I help?](#contributing)

## <a id="what-imports" href="#what-imports"></a>What imports do I need?

The easiest approach to cats imports is to import everything that's commonly needed:

```tut:silent
import cats._
import cats.data._
import cats.implicits._
```

This should be all that you need, but if you'd like to learn more about the details of imports than you can check out the [import guide](imports.html).

## <a id="future-instances" href="#future-instances"></a>Why can't the compiler find implicit instances for Future?

If you have already followed the [imports advice](#what-imports) but are still getting error messages like `could not find implicit value for parameter e: cats.Monad[scala.concurrent.Future]` or `value |+| is not a member of scala.concurrent.Future[Int]`, then make sure that you have an implicit `scala.concurrent.ExecutionContext` in scope. The easiest way to do this is to `import scala.concurrent.ExecutionContext.Implicits.global`, but note that you may want to use a different execution context for your production application.

## <a id="traverse" href="#traverse"></a>How can I turn my List of `<something>` into a `<something>` of a list?

It's really common to have a `List` of values with types like `Option`, `Xor`, or `Validated` that you would like to turn "inside out" into an `Option` (or `Xor` or `Validated`) of a `List`. The `sequence`, `sequenceU`, `traverse`, and `traverseU` methods are _really_ handy for this. You can read more about them in the [Traverse documentation]({{ site.baseurl }}/tut/traverse.html).

## <a id="simulacrum" href="#simulacrum"></a>What does `@typeclass` mean?

Cats defines and implements numerous type classes. Unfortunately, encoding these type classes in Scala can incur a large amount of boilerplate. To address this, [Simulacrum](https://github.com/mpilquist/simulacrum) introduces `@typeclass`, a macro annotation which generates a lot of this boilerplate. This elevates type classes to a first class construct and increases the legibility and maintainability of the code. Use of Simulacrum also ensures consistency in how the type classes are encoded across a project. Cats uses Simulacrum wherever possible to encode type classes, and you can read more about it at the [project page](https://github.com/mpilquist/simulacrum).

## <a id="kind-projector" href="#kind-projector"></a>What do types like `?` and `λ` mean?

Cats defines a wealth of type classes and type class instances. For a number of the type class and instance combinations, there is a mismatch between the type parameter requirements of the type class and the type parameter requirements of the data type for which the instance is being defined. For example, the [Xor]({{ site.baseurl }}/tut/xor.html) data type is a type constructor with two type parameters. We would like to be able to define a [Monad]({{ site.baseurl }}/tut/monad.html) for `Xor`, but the `Monad` type class operates on type constructors having only one type parameter.

**Enter type lambdas!** Type lambdas provide a mechanism to allow one or more of the type parameters for a particular type constructor to be fixed. In the case of `Xor` then, when defining a `Monad` for `Xor`, we want to fix one of the type parameters at the point where a `Monad` instance is summoned, so that the type parameters line up. As `Xor` is right biased, a type lambda can be used to fix the left type parameter and allow the right type parameter to continue to vary when `Xor` is treated as a `Monad`. The right biased nature of `Xor` is discussed further in the [`Xor` documentation]({{ site.baseurl }}/tut/xor.html).

**Enter [kind-projector](https://github.com/non/kind-projector)!** kind-projector is a compiler plugin which provides a convenient syntax for dealing with type lambdas. The symbols `?` and `λ` are treated specially by kind-projector, and expanded into the more verbose definitions that would be required were it not to be used. You can read more about kind-projector at the [project page](https://github.com/non/kind-projector).

## <a id="machinist" href="#machinist"></a>What does `macro Ops` do? What is `cats.macros.Ops`?

`macro Ops` invokes the [Machinist](https://github.com/typelevel/machinist) Ops macro, and is used in cats in a number of places to enrich types with operations with the minimal possible cost when those operations are called in code. Machinist supports an extension mechanism where users of the macro can provide a mapping between symbolic operator names and method names. The `cats.macros.Ops` class uses this extension mechanism to supply the set of mappings that the cats project is interested in.

More about the history of machinist and how it works can be discovered at the [project page](https://github.com/typelevel/machinist), or [this article on the typelevel blog](http://typelevel.org/blog/2013/10/13/spires-ops-macros.html).

## <a id="contributing" href="#contributing"></a>How can I help?

The cats community welcomes and encourages contributions, even if you are completely new to cats and functional programming. Here are a few ways to help out:

- Find an undocumented method and write a ScalaDoc entry for it. See [Arrow.scala]({{ site.sources }}/core/src/main/scala/cats/arrow/Arrow.scala) for some examples of ScalaDoc entries that use [sbt-doctest](https://github.com/tkawachi/sbt-doctest).
- Look at the [code coverage report](https://codecov.io/github/typelevel/cats?branch=master), find some untested code, and write a test for it. Even simple helper methods and syntax enrichment should be tested.
- Find an [open issue](https://github.com/typelevel/cats/issues?q=is%3Aopen+is%3Aissue+label%3Aready), leave a comment on it to let people know you are working on it, and submit a pull request. If you are new to cats, you may want to look for items with the [low-hanging-fruit](https://github.com/typelevel/cats/issues?q=is%3Aopen+is%3Aissue+label%3A%22low-hanging+fruit%22) label.

See the [contributing guide]({{ site.baseurl }}/contributing.html) for more information.
