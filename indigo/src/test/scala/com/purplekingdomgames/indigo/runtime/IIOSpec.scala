package com.purplekingdomgames.indigo.runtime

import org.scalatest.{FunSpec, Matchers}

class IIOSpec extends FunSpec with Matchers {

  describe("creating and destroying IIO monads") {

    it("should allow creation of an IIO") {
      IIO.pure(10).unsafeRun shouldEqual 10
    }

    it("should allow delayed construction of an IIO") {
      IIO.delay(10).unsafeRun() shouldEqual 10
    }

    it("should not evaluate code on creation of an IIO if delayed") {
      val e = new Exception("test")

      IIO.delay(throw e).attemptRun shouldEqual Left(e)
    }

    it("should be able to recover from errors") {
      IIO.pure(10).recover(IIO.pure(50)).unsafeRun() shouldEqual 10
      IIO.raiseError(new Exception("BOOM")).recover(IIO.pure(50)).unsafeRun() shouldEqual 50
    }

  }

  describe("Functor operations") {

    it("should be a functor") {
      IIO.pure(10).map(_ * 10).unsafeRun() shouldEqual 100
    }

    it("should be a monad") {
      IIO.pure(10).flatMap(i => IIO.pure(i * 10)).unsafeRun() shouldEqual 100
    }

    it("should be flattenable") {
      IIO.pure(IIO.pure("hello")).flatten shouldEqual IIO.pure("hello")
    }

  }

}