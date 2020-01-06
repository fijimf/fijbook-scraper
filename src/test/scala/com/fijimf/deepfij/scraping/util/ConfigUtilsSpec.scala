package com.fijimf.deepfij.scraping.util

import com.fijimf.deepfij.scraping.model.{CasablancaScraper, ScrapingModel, Web1NcaaScraper}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.FunSpec

class ConfigUtilsSpec extends FunSpec {
  val cfg: Config = ConfigFactory.parseString(
    """fijbook: {
      |  scraping: {
      |    scrapers:[
      |      {
      |        season:2020
      |        flavor:Casablanca
      |        cron:"0/5 * * * *"
      |      }
      |      {
      |        season:2019
      |        flavor:Web1
      |        cron:"0 * * * *"
      |      }
      |      {
      |        season:2018
      |        flavor:Web1
      |        cron:"0 * * * *"
      |      }
      |      {
      |        season:2017
      |        flavor:Web1
      |        cron:"0 * * * *"
      |      }
      |      {
      |        season:2016
      |        flavor:Web1
      |        cron:"0 * * * *"
      |      }
      |      {
      |        season:2015
      |        flavor:Web1
      |        cron:"0 * * * *"
      |      }
      |    ]
      |  }
      |}
      |    """.stripMargin)

  describe("ConfigUtil") {
    it(" should parse the list of scrapers out of the config") {
      val scrapers: Map[Int, ScrapingModel[_]] = ConfigUtils.loadScrapers(cfg)
      assert(scrapers.size == 6)
      assert(scrapers.get(2020) === Some(CasablancaScraper(2020)))
      assert(scrapers.get(2015) === Some(Web1NcaaScraper(2015)))
    }
    it(" should parse the list of cron jobs out of the config") {
      val scrapers: Map[Int, String] = ConfigUtils.loadJobs(cfg)
      assert(scrapers.size == 6)
      assert(scrapers.get(2020) === Some("0/5 * * * *"))
      assert(scrapers.get(2015) === Some("0 * * * *"))
    }
  }
}
