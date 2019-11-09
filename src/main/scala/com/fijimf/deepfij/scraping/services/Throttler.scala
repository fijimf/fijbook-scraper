package com.fijimf.deepfij.scraping.services

import cats.effect._
import cats.effect.concurrent.Semaphore
import cats.effect.implicits._
import cats.implicits._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.language.higherKinds

object Throttler {
  val log: Logger = LoggerFactory.getLogger(Throttler.getClass)

  def throttle[A, B, F[_]](f: A => F[B], s: Semaphore[F], d: FiniteDuration)(implicit c: Concurrent[F], t: Timer[F]): A => F[B] = {
    a: A =>
      for {
        _ <- s.acquire
        _ <- (Timer[F].sleep(d) *> s.release).start
        bt <- f(a).start
        b <- bt.join
      } yield {
        b
      }
  }
}


