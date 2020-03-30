package com.fijimf.deepfij.scraping


import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import com.fijimf.deepfij.scraping.model.JobDetail
import com.fijimf.deepfij.scraping.model.ScrapeJob.JobsFilter
import com.fijimf.deepfij.scraping.services.{Scraper, ScrapingRepo}
import com.fijimf.deepfij.scraping.util._
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes, Request}
import org.slf4j.{Logger, LoggerFactory}

object ScrapingRoutes {

  val log: Logger = LoggerFactory.getLogger(ScrapingRoutes.getClass)

  def jobsFilterFromReq[F[_]](req: Request[F])(implicit F: Sync[F]): JobsFilter = {
    val years: Option[NonEmptyList[Int]] = req.multiParams.get("season") match {
      case None => None
      case Some(lst) => lst match {
        case Nil=>None
        case h::t=>Some(NonEmptyList(h,t).map(_.toInt))
      }
    }
    val model: Option[String] = req.params.get("model")
    val completed: Option[Boolean] = req.params.get("completed") match {
      case None => None
      case Some(v) if v.equalsIgnoreCase("false") => Some(false)
      case Some(_) => Some(true)
    }
    JobsFilter(years, model, completed)
  }

  implicit def intEntityEncoder[F[_] : Applicative]: EntityEncoder[F, Int] = jsonEncoderOf

  def healthcheckRoutes[F[_]](scraper:Scraper[F],r: ScrapingRepo[F])(implicit F: Sync[F]): HttpRoutes[F] = {
    val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "status" =>
        for {
          dbStatus<-r.healthcheck
          schedStatus<- scraper.healthcheck
          serverInfo = ServerInfo.fromStatus(Map("database"->dbStatus,"scheduleServer"->schedStatus))
          resp <- if (serverInfo.isOk) Ok(serverInfo) else InternalServerError(serverInfo)
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
          yyyymmdd <- F.delay(req.params.getOrElse("asof", DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())))
          job <- scraper.update(season, yyyymmdd)
          resp <- Ok(job)
        } yield {
          resp
        }
    }
  }

  def jobRoutes[F[_]](repo: ScrapingRepo[F])(implicit F: Sync[F]): HttpRoutes[F] = {
    val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case req@GET -> Root / "jobs" =>
        val f: JobsFilter = jobsFilterFromReq[F](req)
        for {
          jobs <- repo.findJobsByFilter(f)
          resp <- Ok(jobs)
        } yield {
          resp
        }

      case GET -> Root / "jobs" / IntVar(jobId) =>
        for {
          job <- repo.findJob(jobId)
          requests <- repo.findRequestByJobId(jobId)
          resp <- job match {
            case Some(j) => Ok(JobDetail(j, requests))
            case None => NotFound(jobId)
          }
        } yield {
          resp
        }
    }
  }


}
