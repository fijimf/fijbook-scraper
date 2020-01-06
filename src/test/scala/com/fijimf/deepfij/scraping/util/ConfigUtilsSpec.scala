package com.fijimf.deepfij.scraping.util

import com.fijimf.deepfij.scraping.model.{CasablancaScraper, ScheduledJob, ScrapingModel, Web1NcaaScraper}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.FunSpec

class ConfigUtilsSpec extends FunSpec {
  val cfg: Config = ConfigFactory.parseString(
    """fijbook: {
      |  scraping: {
      |    scrapers:[
      |      {
      |        season: 2020
      |        model: Casablanca
      |        schedule: [
      |          {
      |            flavor: update
      |            cron: "0/5 * * * *"
      |          }
      |        ]
      |      }
      |      {
      |        season: 2019
      |        model: Web1
      |        schedule: [
      |          {
      |            flavor: fill
      |            cron: "0 * * * *"
      |          }
      |        ]
      |      }
      |      {
      |        season: 2018
      |        model: Web1
      |        flavor: fill
      |        schedule: [
      |          {
      |            flavor: fill
      |            cron: "0 * * * *"
      |          }
      |        ]
      |      }
      |      {
      |        season: 2017
      |        model: Web1
      |        flavor: fill
      |        schedule: [
      |          {
      |            flavor: fill
      |            cron: "0 * * * *"
      |          }
      |        ]
      |      }
      |      {
      |        season: 2016
      |        model: Web1
      |        flavor: fill
      |        schedule: [
      |          {
      |            flavor: fill
      |            cron: "0 * * * *"
      |          }
      |        ]
      |      }
      |      {
      |        season: 2015
      |        model: Web1
      |        flavor: fill
      |        schedule: [
      |          {
      |            flavor: update
      |            cron: "0 * * * *"
      |          }
      |        ]
      |      }
      |    ]
      |  }
      |}""".stripMargin)

  describe("ConfigUtil") {
    it(" should parse the list of scrapers out of the config") {
      val scrapers: Map[Int, ScrapingModel[_]] = ConfigUtils.loadScrapers(cfg)
      assert(scrapers.size == 6)
      assert(scrapers.get(2020) === Some(CasablancaScraper(2020)))
      assert(scrapers.get(2015) === Some(Web1NcaaScraper(2015)))
    }
    it(" should parse the list of cron jobs out of the config") {
      val scrapers: List[ScheduledJob] = ConfigUtils.loadJobs(cfg)
      assert(scrapers.size == 6)
      //TODO
    }
  }
}
