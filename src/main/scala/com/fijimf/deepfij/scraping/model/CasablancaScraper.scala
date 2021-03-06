package com.fijimf.deepfij.scraping.model

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

import cats.implicits._
import com.fijimf.deepfij.schedule.model.UpdateCandidate
import com.fijimf.deepfij.scraping.services.Scraper
import org.slf4j.LoggerFactory

import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success, Try}

case class CasablancaScraper(season: Int) extends DateBasedScrapingModel {
  override val modelName: String = "casablanca"
  override def modelKey(k: LocalDate): String = k.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

  override def urlFromKey(k: LocalDate): String = {
    "https://data.ncaa.com/casablanca/scoreboard/basketball-men/d1/%04d/%02d/%02d/scoreboard.json"
      .format(k.getYear, k.getMonthValue, k.getDayOfMonth)
  }

  override def scrape(key:String, data: String): List[UpdateCandidate] = CasablancaParser.parseGames(data)

  override val rateLimit: Option[(Long, FiniteDuration)] = None
}

object CasablancaParser {
  import io.circe._
  import io.circe.optics.JsonPath._
  import io.circe.parser._

  val log = LoggerFactory.getLogger(CasablancaParser.getClass)

  def parseGames(s: String): List[UpdateCandidate] = {
    parse(s) match {
      case Left(parsingFailure)=>
        log.error(s"Failed to parse JSON ${parsingFailure.getMessage()}")
        List.empty[UpdateCandidate]
      case Right(json)=>
        extractGames(json).flatMap(g => {
          for {
            t <- extractStartTime(g)
            h <- extractHomeTeam(g)
            a <- extractAwayTeam(g)
          } yield {
            parseResult(g) match {
              case Some((hs, as, n)) => UpdateCandidate(t, h, a, None, None, Some(hs), Some(as), Some(n))
              case None => UpdateCandidate(t, h, a, None, None, None, None, None)
            }
          }
        })
    }

  }

  def loadAsJson(s: String): Json = parse(s).getOrElse(Json.Null)

  def extractGames(json: Json): List[Json] = root.games.each.game.json.getAll(json)

  def parseResult(g: Json): Option[(Int, Int, Int)] = {
    if (extractIsFinal(g)) {
      (for {
        h <- extractHomeScore(g)
        a <- extractAwayScore(g)
        n = extractNumPeriods(g)
      } yield {
        (h.toInt, a.toInt, n)
      }).headOption
    } else {
      None
    }
  }

  def extractStartTime(json: Json): List[LocalDateTime] = {
    root.startTimeEpoch.string.getOption(json).map(sec => {
      LocalDateTime.ofInstant(Instant.ofEpochSecond(sec.toLong), ZoneId.of("America/New_York"))
    }).toList
  }

  def extractHomeTeam(json: Json): List[String] = {
    root.home.names.seo.string.getOption(json).toList
  }

  def extractHomeScore(json: Json): List[Int] = {
    Try {
      root.home.score.string.getOption(json).toList.map(_.toInt)
    } match {
      case Success(lst) => lst
      case Failure(exception) =>
        log.warn(s"Failed parsing home score ${exception.getMessage()}")
        List.empty[Int]
    }
  }

  def extractAwayTeam(json: Json): List[String] = {
    root.away.names.seo.string.getOption(json).toList
  }

  def extractAwayScore(json: Json): List[Int] = {
    Try {
      root.away.score.string.getOption(json).toList.map(_.toInt)
    } match {
      case Success(lst) => lst
      case Failure(exception) =>
        log.warn(s"Failed parsing away score ${exception.getMessage()}")
        List.empty[Int]
    }
  }

  def extractNumPeriods(json: Json): Int = {
    root.currentPeriod.string.getOption(json).map(_.toLowerCase) match {
      case None => 2
      case Some(p) if p.contains("5ot") => 7
      case Some(p) if p.contains("4ot") => 6
      case Some(p) if p.contains("3ot") => 5
      case Some(p) if p.contains("2ot") => 4
      case Some(p) if p.contains("ot") => 3
      case _ => 2
    }
  }

  def extractIsFinal(json: Json): Boolean = {
    root.gameState.string.getOption(json) === Some("final")
  }
}
