package com.kenshoo.play.metrics

import play.api.mvc._
import play.api.libs.iteratee.{Step, Input, Done, Iteratee}
import scala.concurrent.{ExecutionContext, Future, Promise}
import play.api.libs.iteratee.Step.Cont

// grabbed this solution from
// https://github.com/playframework/playframework/issues/2401

trait RecoverFilter extends Filter {

  self =>

  def apply(f: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult]

  override def apply(next: EssentialAction): EssentialAction = {
    new EssentialAction {
      import play.api.libs.concurrent.Execution.Implicits.defaultContext

      def apply(rh: RequestHeader): Iteratee[Array[Byte], SimpleResult] = {

        // Promised result, that is returned to the filter when it invokes the wrapped function
        val promisedResult = Promise[SimpleResult]
        // Promised iteratee, that we return to the framework
        val bodyIteratee = Promise[Iteratee[Array[Byte], SimpleResult]]

        // Invoke the filter
        val result = self.apply({ (rh: RequestHeader) =>
        // Invoke the delegate
          bodyIteratee.success(next(rh))
          promisedResult.future
        })(rh)

        result.onComplete({ resultTry =>
        // If we've got a result, but the body iteratee isn't redeemed, then that means the delegate action
        // wasn't invoked.  In which case, we need to supply an iteratee to consume the request body.
          if (!bodyIteratee.isCompleted) {
            bodyIteratee.complete(resultTry.map(simpleResult => Done(simpleResult)))
          }
        })

        // When the iteratee is done, we can redeem the result that was returned to the filter
        Iteratee.flatten(bodyIteratee.future.map(_.mapM({
          simpleResult =>
            promisedResult.success(simpleResult)
            result
        }).recoverM {
          case t: Throwable =>
            // We can now handle failures!
            promisedResult.failure(t)
            result
        }
        ))
      }

    }
  }

  implicit class IterateeWithRecover[E, A](underlying: Iteratee[E, A]) {

    def recover[B >: A](pf: PartialFunction[Throwable, B])(implicit ec: ExecutionContext): Iteratee[E, B] = {
      recoverM { case t: Throwable => Future.successful(pf(t)) }(ec)
    }

    def recoverM[B >: A](pf: PartialFunction[Throwable, Future[B]])(implicit ec: ExecutionContext): Iteratee[E, B] = {
      val pec = ec.prepare()

      def handleErrorMsg(msg: String): Iteratee[E, B] = handleError(new RuntimeException(msg))
      def handleError(t: Throwable): Iteratee[E, B] = Iteratee.flatten(pf(t).map(b => Done[E, B](b))(ec))

      def steps(it: Iteratee[E, A])(input: Input[E]): Iteratee[E, B] = {
        val nextIt = it.pureFlatFold[E, B] {
          case Step.Cont(k) =>
            val n = k(input)
            n.pureFlatFold {
              case Step.Error(msg, _) => handleErrorMsg(msg)
              case other => other.it
            }(ec)
          case Step.Error(msg, _) => handleErrorMsg(msg)
          case other => other.it
        }(ec)

        Iteratee.flatten(
          nextIt.unflatten
            .map(_.it)(ec)
            .recover { case t: Throwable => handleError(t) }(pec)
        )
      }

      Cont(steps(underlying)).it
    }
  }

}
