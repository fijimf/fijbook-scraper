package com.fijimf.deepfij.scraping.services

import cats.Applicative
import cats.effect._
import cats.implicits._
import com.fijimf.deepfij.schedule.model.ScrapeResult
import com.fijimf.deepfij.scraping.model.{DateBasedScrapingModel, ScrapingModel, TeamBasedScrapingModel}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityDecoder.text
import org.http4s.circe.jsonEncoderOf
import org.http4s.client.Client
import org.http4s.{EntityEncoder, Method, Request, Uri}
import org.slf4j.LoggerFactory


case class Scraper[F[_]](httpClient: Client[F], scrapers: Map[Int, ScrapingModel[_]])(implicit F: Async[F], clock: Clock[F]) {
  val log = LoggerFactory.getLogger(Scraper.getClass)

  def scrapeAll(season: Int): F[List[String]] = {
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

  def scrapeUpdate(season: Int): Unit = {
    scrapers.get(season) match {
      case Some(d: DateBasedScrapingModel) =>
      case _ =>
    }
  }

  def createUpdateRequest(sr: ScrapeResult): Request[F] = {
    implicit val scrapeResultEncoder: Encoder.AsObject[ScrapeResult] = deriveEncoder[ScrapeResult]

    implicit def scrapeResultEntityEncoder[F[_] : Applicative]: EntityEncoder[F, ScrapeResult] = jsonEncoderOf

    Request(Method.POST, Uri.uri("http://localhost:8073/update")).withEntity(sr)
  }

  def scrapeUrl(k: String, url: String, model: ScrapingModel[_]): F[String] = {
    for {
      _ <- F.delay(log.info(s"$url"))
      s <- httpClient.expect[String](url)
      sr = ScrapeResult(k, model.scrape(s))
      req = createUpdateRequest(sr)
      t <- httpClient.expect[String](req)
    } yield {
      t
    }
  }

}
object Scraper {

}
