package com.fijimf.deepfij.scraping

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, Timer}
import cats.syntax.semigroupk._
import com.fijimf.deepfij.scraping.model.{CasablancaScraper, ScheduledJob, ScrapingModel, Web1NcaaScraper}
import com.fijimf.deepfij.scraping.services.{Scraper, ScrapingRepo}
import com.fijimf.deepfij.scraping.util.Banner
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.syntax.kleisli._
import org.http4s.{HttpApp, HttpRoutes}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


object ScrapingServer {

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  def stream[F[_] : ConcurrentEffect](transactor: Transactor[F], port: Int, schedHost: String, schedPort: Int, scrapers:Map[Int,ScrapingModel[_]], jobs:List[ScheduledJob])(implicit T: Timer[F], C: ContextShift[F]): Stream[F, ExitCode] = {


    for {
      client <- BlazeClientBuilder[F](global).stream
      repo = ScrapingRepo[F](transactor)
      scraper = Scraper(client, scrapers, repo, schedHost, schedPort)
      _ = ScheduledJob.schedule[F](jobs, scraper)
      scrapingService: HttpRoutes[F] = ScrapingRoutes.scrapeRoutes(scraper)
      healthcheckService: HttpRoutes[F] = ScrapingRoutes.healthcheckRoutes(scraper,repo)
      httpApp: HttpApp[F] = (healthcheckService <+> scrapingService).orNotFound
      finalHttpApp: HttpApp[F] = Logger.httpApp[F](logHeaders = true, logBody = true)(httpApp)
      exitCode <- BlazeServerBuilder[F]
        .bindHttp(port = port, host = "0.0.0.0")
        .withIdleTimeout(1.minutes)
        .withResponseHeaderTimeout(5.minutes)
        .withHttpApp(finalHttpApp)
          .withBanner(Banner.banner)
        .serve
    } yield {
      exitCode
    }
    }.drain


}
