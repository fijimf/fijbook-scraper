package com.fijimf.deepfij.scraping.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.Applicative
import cats.effect._
import cats.implicits._
import com.fijimf.deepfij.schedule.model.ScrapeResult
import com.fijimf.deepfij.scraping.model.{DateBasedScrapingModel, ScrapingModel, TeamBasedScrapingModel}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.apache.commons.codec.digest.DigestUtils
import org.http4s.EntityDecoder.text
import org.http4s.circe.jsonEncoderOf
import org.http4s.client.Client
import org.http4s.{EntityEncoder, Method, ParseResult, Request, Response, Status, Uri}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._


case class Scraper[F[_]](httpClient: Client[F], scrapers: Map[Int, ScrapingModel[_]])(implicit F: Async[F], clock: Clock[F]) {
  val log = LoggerFactory.getLogger(Scraper.getClass)

  def fill(season: Int): F[List[String]] = {
    scrapers.get(season) match {
      case Some(d: DateBasedScrapingModel) =>
        F.delay(log.info(s"For season $season found model ${d.modelName}."))
        val k: List[F[String]] = d.keys.map(k => {
          val url: String = d.urlFromKey(k)
          scrapeUrl(d.modelKey(k), url, d)
        })
        k.sequence
      case Some(t: TeamBasedScrapingModel) => F.delay(List("Unable to handle team models"))
      case _ => F.delay(List("Could not find appropriate model"))
    }

  }

  def update(season: Int, yyyymmdd:String): F[List[String]]  = {
    val asOf: LocalDate = LocalDate.parse(yyyymmdd,DateTimeFormatter.ofPattern("yyyyMMdd"))
    scrapers.get(season) match {
      case Some(d: DateBasedScrapingModel) =>
        F.delay(log.info(s"For season $season found model ${d.modelName}."))
        F.delay(
          log.info(s"Running update based on date $yyyymmdd.")
        )
        val k: List[F[String]] = d.updateKeys(asOf).map(k => {
          val url: String = d.urlFromKey(k)
          scrapeUrl(d.modelKey(k), url, d)
        })
        k.sequence
      case Some(t: TeamBasedScrapingModel) => F.delay(List("Team based models do not support update"))
      case _ => F.delay(List("Could not find appropriate model"))
    }
  }

  def createUpdateRequest(sr: ScrapeResult): Request[F] = {
//    implicit val scrapeResultEncoder: Encoder.AsObject[ScrapeResult] = deriveEncoder[ScrapeResult]
//
//    implicit def scrapeResultEntityEncoder[F[_] : Applicative]: EntityEncoder[F, ScrapeResult] = jsonEncoderOf

    Request(Method.POST, Uri.uri("http://localhost:8074/update")).withEntity(sr)
  }

  def handleRawResponse(key: String, start: Long, resp: Response[F]): F[(ScrapeDataRetrieval, Option[String])] = {
    val statusCode: Int = resp.status.code
    val ok: Boolean = statusCode === Status.Ok.code
    for {
      end <- clock.realTime(MILLISECONDS)
      data <- if (ok) resp.as[String] else F.pure("")
    } yield {
      (ScrapeDataRetrieval(key, statusCode, end - start, data.length, DigestUtils.md5Hex(data)), if (ok) Some(data) else None)
    }
  }


  def scrapeUrl(k: String, url: String, model: ScrapingModel[_]): F[String] = {
    Uri.fromString(url) match {
      case Left(parseFailure)=>
        F.delay(s"$k => ${parseFailure.getMessage()}")
      case Right(uri) =>
        for {
          start <- clock.realTime(MILLISECONDS)
          _ <- F.delay(log.info(s"$url"))
          (sd, data) <- httpClient.fetch(Request[F](Method.GET, uri))(handleRawResponse(k, start, _))
          sr = data.map(s => ScrapeResult(k, model.scrape(s)))
          _<- F.delay(log.info(s"$k=>$sd ${data.map(_.length).getOrElse(-1)}  ${sr.map(_.updates.size).getOrElse(0)}"))
          req = sr.map(createUpdateRequest)
          t <- req match {
            case Some(r) => httpClient.expect[String](r)
            case _ => F.delay(sd.toString)
          }
        } yield {
          s"$k => $t"
        }
    }

  }

}
case class ScrapeDataRetrieval(key:String, statusCode:Int, requestLatency:Long, size:Int, digest:String)