package com.fijimf.deepfij.scraping

import cats.effect._
import cats.syntax.semigroupk._
import com.fijimf.deepfij.scraping.model.{JobScheduler, ScheduledJob, ScrapingModel}
import com.fijimf.deepfij.scraping.services.{Scraper, ScrapingRepo}
import com.fijimf.deepfij.scraping.util.Banner
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s.client.Client
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.syntax.kleisli._
import org.http4s.{HttpApp, HttpRoutes}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._


object ScrapingServer {
  val logger: org.slf4j.Logger = LoggerFactory.getLogger(ScrapingServer.getClass)

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  def stream[F[_] : ConcurrentEffect](transactor: Transactor[F], scraper:Scraper[F], repo:ScrapingRepo[F], port: Int, client: Client[F], schedHost: String, schedPort: Int, scrapers: Map[Int, ScrapingModel[_]])(implicit T: Timer[F], C: ContextShift[F]): Stream[F, ExitCode] = {


    val scrapingService: HttpRoutes[F] = ScrapingRoutes.scrapeRoutes(scraper)
    val healthcheckService: HttpRoutes[F] = ScrapingRoutes.healthcheckRoutes(scraper, repo)
    val jobsService: HttpRoutes[F] = ScrapingRoutes.jobRoutes(repo)
    val httpApp: HttpApp[F] = (healthcheckService <+> scrapingService <+> jobsService).orNotFound
    val finalHttpApp: HttpApp[F] = Logger.httpApp[F](logHeaders = true, logBody = true)(httpApp)



    for {
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
