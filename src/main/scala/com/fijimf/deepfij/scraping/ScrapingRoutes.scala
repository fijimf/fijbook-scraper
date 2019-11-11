package com.fijimf.deepfij.scraping


import com.fijimf.deepfij.scraping.model._

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import com.fijimf.deepfij.scraping.services.Scraper
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes}
import org.slf4j.{Logger, LoggerFactory}

object ScrapingRoutes {

  val log: Logger = LoggerFactory.getLogger(ScrapingRoutes.getClass)

  implicit def intEntityEncoder[F[_] : Applicative]: EntityEncoder[F, Int] = jsonEncoderOf

  def healthcheckRoutes[F[_]]()(implicit F: Sync[F]): HttpRoutes[F] = {
    val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "healthcheck" =>
        for {
          resp <- Ok()
        } yield {
          resp
        }
    }
  }

  def scrapeRoutes[F[_]](scraper: Scraper[F])(implicit F: Sync[F]): HttpRoutes[F] = {
    val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "fill" / IntVar(season) =>
        for {
          job<-scraper.fill(season)
          resp <- Ok(job)
        } yield {
          resp
        }
      case req@GET -> Root / "update" / IntVar(season) =>
        for {
          yyyymmdd <- F.delay( req.params.getOrElse("asof", DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())))
          job<-scraper.update(season, yyyymmdd)
          resp <- Ok(job)
        } yield {
          resp
        }
    }
  }


}
