package com.fijimf.deepfij.scraping.model

import com.fijimf.deepfij.schedule.model.UpdateCandidate
import org.scalatest.FunSpec

import scala.io.Source
import scala.util.{Failure, Success}
import scala.xml.{Elem, NodeSeq, XML}

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
      val node: Elem = XML.loadString(
        """ <tr class="text">
          |                        <td width="75%">
          |                            <a class="schoolColorsLink" href="javascript:showStats();">Baylor</a>
          |                        </td>
          |                        <td align="right">19</td>
          |                        <td align="right">15</td>
          |                        <td align="right">0</td>
          |                        <td align="right">34</td>
          |                    </tr>""")
      assert(Web1NcaaParser.checkTdForPercent(node)===false)
    }

    it ("It can test <tr> nodes for percent in the first <td> child (true)"){
      val node: Elem = XML.loadString(
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
                    </tr>""".stripMargin)
      assert(Web1NcaaParser.checkTdForPercent(node)===true)

    }

    it("It extracts the correct number of game rows") {
      Web1NcaaParser.loadFromString(baylor) match {
        case Failure(thr) => fail(thr)
        case Success(root) =>
           val seq: NodeSeq = Web1NcaaParser.extractGameRows(root)
          assert(seq.size===34)
      }
      assert(youngstownSt.length > 0)
      Web1NcaaParser.loadFromString(youngstownSt) match {
        case Failure(thr) => fail(thr)
        case Success(root) =>
          val seq: NodeSeq = Web1NcaaParser.extractGameRows(root)
          assert(seq.size===32)

      }
    }
    it("It correctly extracts games") {
      Web1NcaaParser.loadFromString(baylor) match {
        case Failure(thr) => fail(thr)
        case Success(root) =>
          val candidates: List[UpdateCandidate] = Web1NcaaParser.extractUpdates("51", root)
          assert(candidates.size === 33)
      }
      Web1NcaaParser.loadFromString(youngstownSt) match {
        case Failure(thr) => fail(thr)
        case Success(root) =>
          val candidates: List[UpdateCandidate] = Web1NcaaParser.extractUpdates("817", root)
          assert(candidates.size === 30)
      }
    }
  }

}
