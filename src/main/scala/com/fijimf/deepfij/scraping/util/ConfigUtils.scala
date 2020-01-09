package com.fijimf.deepfij.scraping.util

import com.fijimf.deepfij.scraping.model.{CasablancaScraper, ScheduledJob, ScrapingModel, Web1NcaaScraper}
import com.fijimf.deepfij.scraping.services.Scraper
import com.typesafe.config.Config

import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

object ConfigUtils {
  val log: Logger = LoggerFactory.getLogger(ConfigUtils.getClass)
  def loadScrapers(config: Config): Map[Int, ScrapingModel[_]] = {
    config
      .getConfigList("fijbook.scraping.scrapers")
      .asScala
      .foldLeft(Map.empty[Int, ScrapingModel[_]]) { case (map: Map[Int, ScrapingModel[_]], cfg: Config) => {
        val year: Int = cfg.getInt("season")
        cfg.getString("model") match {
          case "Casablanca" =>
            log.info(s"For season $year creating a Casablanca scraper")
            map + (year -> CasablancaScraper(year))
          case "Web1" =>
            log.info(s"For season $year creating a Web1NcaaScraper scraper")
            map + (year -> Web1NcaaScraper(year))
          case _ => map
        }
      }
      }
  }

  def loadJobs(config: Config): List[ScheduledJob] = {
    config
      .getConfigList("fijbook.scraping.scrapers")
      .asScala
      .foldLeft(List.empty[ScheduledJob]) { case (lst: List[ScheduledJob], cfg: Config) =>
        val jobs1: List[ScheduledJob] = cfg.getConfigList("schedule")
          .asScala
          .foldLeft(List.empty[ScheduledJob]) { case (jobs: List[ScheduledJob], cfgInner: Config) =>
            ScheduledJob(
              cfg.getInt("season"),
              cfgInner.getString("cron"),
              cfgInner.getString("flavor")
            ) :: jobs
          }
        jobs1 ::: lst
      }
  }
}
