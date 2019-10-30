package com.fijimf.deepfij.scraping.model

import java.time.LocalDate

trait DateBasedScrapingModel extends ScrapingModel[LocalDate] {
  def modelKey(k: LocalDate): String

  val start: LocalDate = LocalDate.of(season - 1, 11, 1)
  val end: LocalDate = LocalDate.of(season, 5, 1)
  val keys: List[LocalDate] = Iterator.iterate[LocalDate](start)(_.plusDays(1)).takeWhile(_.isBefore(end)).toList
  val lookBackDays = 3
  val lookAheadDays = 8

  def updateKeys(d: LocalDate): List[LocalDate] = keys.filter(d => {
    d.isAfter(d.minusDays(lookBackDays)) && d.isBefore(d.plusDays(lookAheadDays))
  })

  def updateUrls(d:LocalDate):List[String] = updateKeys(d).map(urlFromKey)
}
