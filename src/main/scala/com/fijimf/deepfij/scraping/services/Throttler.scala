package com.fijimf.deepfij.scraping.services

import cats.effect._
import cats.effect.implicits._
import cats._
import cats.data._
import cats.implicits._
import cats.effect.concurrent.Semaphore
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.higherKinds

object Throttler {
  val log = LoggerFactory.getLogger(Throttler.getClass)
  def throttle[A, B, F[_]](f: A => F[B], s: Semaphore[F], d: FiniteDuration)(implicit c:Concurrent[F], t: Timer[F]): A => F[B] = {
    val f1: A => F[B] = a => for {
      _ <- s.available.map(n=>log.info(s"Available $n"))
      _ <- s.acquire
      _ <- s.available.map(n=>log.info(s"Available $n"))
      _ <- (Timer[F].sleep(d) *> s.release).start
      bt <- f(a).start
      b <- bt.join
    } yield {
      b
    }
    f1
  }
}


