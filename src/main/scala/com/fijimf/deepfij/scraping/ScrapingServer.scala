package com.fijimf.deepfij.scraping

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, Timer}
import cats.syntax.semigroupk._
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.syntax.kleisli._
import org.http4s.{HttpApp, HttpRoutes}


object ScrapingServer {

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  def stream[F[_] : ConcurrentEffect](transactor: Transactor[F])(implicit T: Timer[F], C: ContextShift[F]): Stream[F, ExitCode] = {
    val healthcheckService: HttpRoutes[F] = ScrapingRoutes.healthcheckRoutes()
     val httpApp: HttpApp[F] = (
      healthcheckService ).orNotFound
    val finalHttpApp: HttpApp[F] = Logger.httpApp[F](logHeaders = true, logBody = true)(httpApp)
    for {
      exitCode <- BlazeServerBuilder[F]
        .bindHttp(port = 8073, host = "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield {
      exitCode
    }
    }.drain


}
