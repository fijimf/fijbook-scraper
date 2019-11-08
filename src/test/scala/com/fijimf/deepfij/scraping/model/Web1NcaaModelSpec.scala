package com.fijimf.deepfij.scraping.model

import org.scalatest.FunSpec

import scala.io.Source
import scala.util.{Failure, Success}
import scala.xml.NodeSeq

class Web1NcaaModelSpec extends FunSpec {
  val baylor: String = Source.fromResource("orgId51.html").mkString
  val youngstownSt: String = Source.fromResource("orgId817.html").mkString

  describe("Web1Ncaa scraper") {
    it("key-code map is bijective") {
      assert(Web1NcaaKey.codeToKey.size===Web1NcaaKey.keyToCode.size)
    }

    it("Parses as valid HTML") {
      assert(baylor.length > 0)
      assert(youngstownSt.length > 0)
      Web1NcaaParser.loadFromString(baylor) match {
        case Failure(thr) => fail(thr)
        case _ => //OK
      }
      Web1NcaaParser.loadFromString(youngstownSt) match {
        case Failure(thr) => fail(thr)
        case _ => //OK

      }
    }

    it ("It can test <tr> nodes for percent in the first <td> child (false)"){
      Web1NcaaParser.loadFromString(
      """ <tr class="text">
        |                        <td width="75%">
        |                            <a class="schoolColorsLink" href="javascript:showStats();">Baylor</a>
        |                        </td>
        |                        <td align="right">19</td>
        |                        <td align="right">15</td>
        |                        <td align="right">0</td>
        |                        <td align="right">34</td>
        |                    </tr>""".stripMargin) match {
        case Failure(thr) => fail(thr)
        case Success(node) =>
          assert(Web1NcaaParser.checkTdForPercent(node)===false)
      }
    }
    it ("It can test <tr> nodes for percent in the first <td> child (true)"){
      Web1NcaaParser.loadFromString(
      """<tr class="text">
                        <td align="left">
                            %
                            <a class="schoolColorsLink" href="javascript:showTeamResults(1004);">Central Ark.</a>
                        </td>
                        <td align="center">11/10/2017</td>
                        <td align="center" class=" schoolColorsLink ">107</td>
                        <td align="center" class=" text ">66</td>
                        <td align="center">
                            Home
                        </td>
                        <td align="center"></td>
                        <td align="center">
                            -
                        </td>
                        <td align="right">7,791</td>
                    </tr>""".stripMargin) match {
        case Failure(thr) => fail(thr)
        case Success(node) =>
          assert(Web1NcaaParser.checkTdForPercent(node)===true)
      }
    }

    it("It extracts the correct number of game rows") {
      Web1NcaaParser.loadFromString(baylor) match {
        case Failure(thr) => fail(thr)
        case Success(root) =>
           val seq: NodeSeq = Web1NcaaParser.extractGameRows(root)
          seq.toList.zipWithIndex.foreach(tup=>println(s"${tup._2} ==> ${tup._1.text}"))
          assert(seq.size===33)
      }
      assert(youngstownSt.length > 0)
      Web1NcaaParser.loadFromString(youngstownSt) match {
        case Failure(thr) => fail(thr)
        case Success(root) =>
          val seq: NodeSeq = Web1NcaaParser.extractGameRows(root)
          assert(seq.size===32)

      }
    }

    //    it("Extracts game nodes as Json") {
    //      val json: Json = CasablancaParser.loadAsJson(cb0205)
    //      val gameJsons: List[Json] = CasablancaParser.extractGames(json)
    //      assert(gameJsons.size === 24)
    //    }
    //
    //    it("Extracts a valid start time for each game") {
    //      val json: Json = CasablancaParser.loadAsJson(cb0205)
    //      val gameJsons: List[Json] = CasablancaParser.extractGames(json)
    //      val dateTimes: List[LocalDateTime] = gameJsons.flatMap(j => {
    //        CasablancaParser.extractStartTime(j)
    //      })
    //
    //      assert(gameJsons.size === dateTimes.size)
    //      dateTimes.foreach(d => {
    //        assert(d.toLocalDate === LocalDate.of(2019, 2, 5))
    //      })
    //    }
    //    it("Extracts a valid home and away teams for each game") {
    //      val json: Json = CasablancaParser.loadAsJson(cb0205)
    //      val gameJsons: List[Json] = CasablancaParser.extractGames(json)
    //      gameJsons.foreach(g => {
    //        val hl: List[String] = CasablancaParser.extractHomeTeam(g)
    //        val al: List[String] = CasablancaParser.extractAwayTeam(g)
    //        assert(hl.size === 1)
    //        assert(al.size === 1)
    //        assert(!(hl === al))
    //      })
    //    }
    //
    //    it("Extracts a valid home and away scores for each completed game") {
    //      val json: Json = CasablancaParser.loadAsJson(cb0205)
    //      val gameJsons: List[Json] = CasablancaParser.extractGames(json)
    //      gameJsons.foreach(g => {
    //        val hs: List[Int] = CasablancaParser.extractHomeScore(g).map(_.toInt)
    //        val as: List[Int] = CasablancaParser.extractAwayScore(g).map(_.toInt)
    //        assert(hs.size === 1)
    //        assert(as.size === 1)
    //        assert(hs.forall(_ > 0))
    //        assert(as.forall(_ > 0))
    //      })
    //    }
    //
    //    it("Extracts the number of periods for each completed game") {
    //      val json: Json = CasablancaParser.loadAsJson(cb0205)
    //      val gameJsons: List[Json] = CasablancaParser.extractGames(json)
    //      val ns = gameJsons.map(g => {
    //        CasablancaParser.extractNumPeriods(g)
    //      })
    //      assert(ns.size === gameJsons.size)
    //      assert(ns.forall(_ >= 2))
    //    }
    //
    //    it("Extracts games for valid JSON"){
    //      val candidates: List[UpdateCandidate] = CasablancaParser.parseGames(cb0205)
    //candidates.foreach(println(_))
    //      assert(candidates.size === 24)
    //
    //    }
  }

}
