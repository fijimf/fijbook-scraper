package com.fijimf.deepfij.scraping.services

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import cats.effect._
import cats.implicits._
import com.fijimf.deepfij.schedule.model.ScrapeResult
import com.fijimf.deepfij.scraping.model._
import org.apache.commons.codec.digest.DigestUtils
import org.http4s.EntityDecoder.text
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.{EntityDecoder, EntityEncoder, Header, Method, Request, Response, Status, Uri, _}
import org.slf4j.{Logger, LoggerFactory}


case class Scraper[F[_]](httpClient: Client[F], scrapers: Map[Int, ScrapingModel[_]], repo: ScrapingRepo[F])(implicit F: ConcurrentEffect[F], cs: ContextShift[F], clock: Clock[F], tim: Timer[F]) {
  val log: Logger = LoggerFactory.getLogger(Scraper.getClass)
  val header: Header = Header.apply("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36")

  implicit def intEntityEncoder: EntityEncoder[F, Int] = jsonEncoderOf

  implicit def intEntityDecoder: EntityDecoder[F, Int] = jsonOf

  //TODO
  // 2. Add throttling back
  // 3. Add the optimizations based on digest

  def fill(season: Int): F[List[ScrapeRequest]] = {
    scrapers.get(season) match {
      case Some(d: ScrapingModel[_]) =>
        F.delay(log.info(s"For season $season found model ${d.modelName}."))
        buildFillJob(season, d)
      case _ =>
        F.delay(List("Could not find appropriate model"))
        F.pure(List.empty[ScrapeRequest])
    }

  }

  def update(season: Int, yyyymmdd:String): F[List[ScrapeRequest]] = {
    val asOf: LocalDate = LocalDate.parse(yyyymmdd,DateTimeFormatter.ofPattern("yyyyMMdd"))
    scrapers.get(season) match {
      case Some(d: DateBasedScrapingModel) =>
        F.delay(log.info(s"For season $season found model ${d.modelName}."))
        buildUpdateJob(season, d, asOf)
      case _ =>
        F.delay(List("Could not find appropriate model"))
        F.pure(List.empty[ScrapeRequest])
    }

  }

  def buildUpdateJob[T](season: Int, m: DateBasedScrapingModel, asOf:LocalDate): F[List[ScrapeRequest]] = {
    ScrapeJob(0L, "fill", season, m.modelName, LocalDateTime.now(), None)
    for {
      _ <- F.delay(log.info(s"For season $season found model ${m.modelName}."))
      sj <- repo.insertScrapeJob(ScrapeJob(0L, "fill", season, m.modelName, LocalDateTime.now(), None))
      f = buildFunction(m, sj)
      resps <- m.updateKeys(asOf).map(f).sequence
    } yield {
      resps
    }
  }

  def buildFillJob[T](season: Int, m: ScrapingModel[T]): F[List[ScrapeRequest]] = {
    ScrapeJob(0L, "fill", season, m.modelName, LocalDateTime.now(), None)
    for {
      _ <- F.delay(log.info(s"For season $season found model ${m.modelName}."))
      sj <- repo.insertScrapeJob(ScrapeJob(0L, "fill", season, m.modelName, LocalDateTime.now(), None))
      f = buildFunction(m, sj)
      resps <- m.keys.map(f).sequence
    } yield {
      resps
    }
  }

  def buildFunction[T](m: ScrapingModel[T], j: ScrapeJob): T => F[ScrapeRequest] = {
    def handleRawResponse(sr: ScrapeRequest, resp: Response[F]): F[ScrapeRequest] = {
      val statusCode: Int = resp.status.code
      if (statusCode === Status.Ok.code) {
        for {
          data <- resp.as[String]
          result = ScrapeResult(sr.modelKey, m.scrape(sr.modelKey, data))
          updateRequest = createUpdateRequest(result)
          updatesMade <- httpClient.expect[Int](updateRequest)
          savedResult <- repo.insertScrapeRequest(
            sr.copy(
              statusCode = statusCode,
              digest = DigestUtils.md5Hex(data),
              updatesProposed = result.updates.size,
              updatesAccepted = updatesMade
            )
          )
        } yield {
          savedResult
        }
      } else {
        for {
          savedResult <- repo.insertScrapeRequest(sr)
        } yield {
          savedResult
        }
      }
    }

    t: T => {
      val sr = ScrapeRequest(0L, j.id, m.modelKey(t), LocalDateTime.now(), -1, "", -1, -1)
      Uri.fromString(m.urlFromKey(t)) match {
        case Left(_) => repo.insertScrapeRequest(sr)
        case Right(uri) =>
          httpClient.fetch(
            Request[F](Method.POST, uri).withHeaders(header)
          )(handleRawResponse(sr, _))
      }
    }
  }

  def createUpdateRequest(sr: ScrapeResult): Request[F] = {
    Request(Method.POST, uri"http://localhost:8074/update").withEntity(sr)
  }

}