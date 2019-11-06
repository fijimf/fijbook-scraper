package com.fijimf.deepfij.scraping

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, Timer}
import cats.syntax.semigroupk._
import com.fijimf.deepfij.scraping.model.{CasablancaScraper, ScrapingModel, Web1NcaaScraper}
import com.fijimf.deepfij.scraping.services.Scraper
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.syntax.kleisli._
import org.http4s.{HttpApp, HttpRoutes}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object ScrapingServer {

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  def stream[F[_] : ConcurrentEffect](transactor: Transactor[F])(implicit T: Timer[F], C: ContextShift[F]): Stream[F, ExitCode] = {
    val healthcheckService: HttpRoutes[F] = ScrapingRoutes.healthcheckRoutes()

    for {
      client <- BlazeClientBuilder[F](global).stream
      scraper = Scraper(client, Map(2019-> CasablancaScraper(2019), 2018->Web1NcaaScraper(2018)))
      scrapingService: HttpRoutes[F] = ScrapingRoutes.scrapeRoutes(scraper)
      httpApp: HttpApp[F] = (healthcheckService <+> scrapingService).orNotFound
      finalHttpApp: HttpApp[F] = Logger.httpApp[F](logHeaders = true, logBody = true)(httpApp)
      exitCode <- BlazeServerBuilder[F]
        .bindHttp(port = 8077, host = "0.0.0.0")
        .withIdleTimeout(1.minutes)
        .withResponseHeaderTimeout(5.minutes)
        .withHttpApp(finalHttpApp)
        .serve
    } yield {
      exitCode
    }
    }.drain


}
