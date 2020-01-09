package com.fijimf.deepfij.scraping.model

import java.time.LocalDateTime

import cats.effect._
import cats.effect.implicits._
import cats.implicits._

import scala.concurrent.duration._

object SchedPOC extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val scheduler: Scheduler[IO] = Scheduler[IO]()
    for {
      _ <- scheduler.schedule(IO.delay(println("Every 10 secs")), 10000L)
      _ <- scheduler.schedule(IO.delay(println("Every 5 secs")), 5000)
      _ <- scheduler.schedule(IO.delay(println("Every 1 sec")), 1000)
      _ <- IO.delay(println(s"${LocalDateTime.now} ${Thread.currentThread().getName}  All scheduled"))
      _ <- IO.never
      exitCode = ExitCode(0)
    } yield {
      exitCode
    }
  }
}

case class Scheduler[F[+_]]()(implicit c: Concurrent[F], cs: ContextShift[F], clock: Clock[F], tim: Timer[F]) {

  def repeat(io: F[_], millis: Long): F[Nothing] = {
    tim.sleep(millis.millis)  *> io >> repeat(io, millis)
  }

  def schedule(io:F[_], ms: Long)(implicit c: Concurrent[F], cs: ContextShift[F], clock: Clock[F], tim: Timer[F]): F[Fiber[F, Nothing]] = {
    for {
      _ <- c.delay(println(s"${LocalDateTime.now} ${Thread.currentThread().getName}  Basic Setup..."))
      fooThread: Fiber[F, Nothing] <- repeat(io, ms).start
    } yield {
      fooThread
    }
  }
}
