package com.fijimf.deepfij.scraping.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.effect._
import cats.effect.concurrent.Semaphore
import cats.implicits._
import com.fijimf.deepfij.schedule.model.ScrapeResult
import com.fijimf.deepfij.scraping.model.{DateBasedScrapingModel, ScrapingModel, TeamBasedScrapingModel}
import org.apache.commons.codec.digest.DigestUtils
import org.http4s.EntityDecoder.text
import org.http4s.client.Client
import org.http4s.{Header, Method, Request, Response, Status, Uri}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._


case class Scraper[F[_]](httpClient: Client[F], scrapers: Map[Int, ScrapingModel[_]])(implicit F: ConcurrentEffect[F], cs:ContextShift[F], clock: Clock[F], tim:Timer[F]) {
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
      case Some(t: TeamBasedScrapingModel) =>
        for {
          _ <- F.delay(log.info(s"For season $season found model ${t.modelName}."))
          sem <- Semaphore(1L)
          lk <- t.keys.map(k => {
            val url: String = t.urlFromKey(k)
            scrapeUrl(t.modelKey(k), url, t, sem)
          }).sequence
        } yield {
          lk
        }
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
      println(s"--->>|$data|<<---")
      (ScrapeDataRetrieval(key, statusCode, end - start, data.length, DigestUtils.md5Hex(data)), if (ok) Some(data) else None)
    }
  }

  def scrapeUrl(k: String, url: String, model: ScrapingModel[_]): F[String] = {
    for {
      sem<-Semaphore(9999)//<-TODO remove this silly hack
      res<-scrapeUrl(k, url, model, sem)
    } yield{
      res
    }
  }

  def scrapeUrl(k: String, url: String, model: ScrapingModel[_], sem:Semaphore[F]): F[String] = {
    Uri.fromString(url) match {
      case Left(parseFailure)=>
        F.delay(s"$k => ${parseFailure.getMessage()}")
      case Right(uri) =>
        for {
          start <- clock.realTime(MILLISECONDS)
          scrapeFunction: (Uri => F[(ScrapeDataRetrieval, Option[String])]) = (u :Uri)=> {httpClient.fetch(
            Request[F](Method.POST, u).withHeaders(Header.apply("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36"))
          )(handleRawResponse(k, start, _))}
          throttledScrapeFunction = Throttler.throttle(scrapeFunction,sem, 1.second)
          _ <- F.delay(log.info(s"$url"))
          (sd, data) <- throttledScrapeFunction(uri)
          sr = data.map(s => ScrapeResult(k, model.scrape(k, s)))
          _<- F.delay(log.info(s"$k=>$sd ${data.map(_.length).getOrElse(-1)}  ${sr.map(_.updates.size).getOrElse(0)}"))
          _<- F.delay(log.info(s"\n${sr.map(_.updates.mkString("\n"))}"))
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