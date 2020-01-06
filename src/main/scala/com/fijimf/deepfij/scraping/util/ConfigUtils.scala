package com.fijimf.deepfij.scraping.util

import com.fijimf.deepfij.scraping.model.{CasablancaScraper, ScrapingModel, Web1NcaaScraper}
import com.typesafe.config.Config

import scala.collection.JavaConverters._

object ConfigUtils {
  def loadScrapers(config: Config): Map[Int, ScrapingModel[_]] = {
    config
      .getConfigList("fijbook.scraping.scrapers")
      .asScala
      .foldLeft(Map.empty[Int, ScrapingModel[_]]) { case (map: Map[Int, ScrapingModel[_]], cfg: Config) => {
        val year: Int = cfg.getInt("season")
        cfg.getString("flavor") match {
          case "Casablanca" => map + (year -> CasablancaScraper(year))
          case "Web1" => map + (year -> Web1NcaaScraper(year))
          case _ => map
        }
      }
      }
  }

  def loadJobs(config: Config): Map[Int, String] = {
    config
      .getConfigList("fijbook.scraping.scrapers")
      .asScala
      .foldLeft(Map.empty[Int, String]) { case (map: Map[Int, String], cfg: Config) => {
        map + (cfg.getInt("season") -> cfg.getString("cron"))
      }
      }
  }
}
