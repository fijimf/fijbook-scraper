package com.fijimf.deepfij.scraping.model

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.schedule.model.UpdateCandidate
import io.circe.Json
import org.scalatest.FunSpec

import scala.io.Source

class CasablancaSpec extends FunSpec {
  val cb0205: String = Source.fromResource("casablanca20190205.json").mkString

  describe("Casablanca scraper") {
    it("Parses as valid JSON") {
      assert(cb0205.length > 0)
      assert(!(CasablancaParser.loadAsJson(cb0205) === Json.Null))
    }

    it("Extracts game nodes as Json") {
      val json: Json = CasablancaParser.loadAsJson(cb0205)
      val gameJsons: List[Json] = CasablancaParser.extractGames(json)
      assert(gameJsons.size === 24)
    }

    it("Extracts a valid start time for each game") {
      val json: Json = CasablancaParser.loadAsJson(cb0205)
      val gameJsons: List[Json] = CasablancaParser.extractGames(json)
      val dateTimes: List[LocalDateTime] = gameJsons.flatMap(j => {
        CasablancaParser.extractStartTime(j)
      })

      assert(gameJsons.size === dateTimes.size)
      dateTimes.foreach(d => {
        assert(d.toLocalDate === LocalDate.of(2019, 2, 5))
      })
    }
    it("Extracts a valid home and away teams for each game") {
      val json: Json = CasablancaParser.loadAsJson(cb0205)
      val gameJsons: List[Json] = CasablancaParser.extractGames(json)
      gameJsons.foreach(g => {
        val hl: List[String] = CasablancaParser.extractHomeTeam(g)
        val al: List[String] = CasablancaParser.extractAwayTeam(g)
        assert(hl.size === 1)
        assert(al.size === 1)
        assert(!(hl === al))
      })
    }

    it("Extracts a valid home and away scores for each completed game") {
      val json: Json = CasablancaParser.loadAsJson(cb0205)
      val gameJsons: List[Json] = CasablancaParser.extractGames(json)
      gameJsons.foreach(g => {
        val hs: List[Int] = CasablancaParser.extractHomeScore(g).map(_.toInt)
        val as: List[Int] = CasablancaParser.extractAwayScore(g).map(_.toInt)
        assert(hs.size === 1)
        assert(as.size === 1)
        assert(hs.forall(_ > 0))
        assert(as.forall(_ > 0))
      })
    }

    it("Extracts the number of periods for each completed game") {
      val json: Json = CasablancaParser.loadAsJson(cb0205)
      val gameJsons: List[Json] = CasablancaParser.extractGames(json)
      val ns = gameJsons.map(g => {
        CasablancaParser.extractNumPeriods(g)
      })
      assert(ns.size === gameJsons.size)
      assert(ns.forall(_ >= 2))
    }

    it("Extracts games for valid JSON"){
      val candidates: List[UpdateCandidate] = CasablancaParser.parseGames(cb0205)
candidates.foreach(println(_))
      assert(candidates.size === 24)

    }
  }

}
