package com.fijimf.deepfij.scraping.services

import java.sql.Timestamp
import java.time.LocalDateTime

import com.fijimf.deepfij.scraping.model.ScrapeJob
import doobie.util.Meta

trait AbstractScrapingRepo[F[_]] {
  implicit val localDateTimeMeta: Meta[LocalDateTime] = Meta[Timestamp].imap(ts => ts.toLocalDateTime)(ldt => Timestamp.valueOf(ldt))

  def listScrapeJobs():F[List[ScrapeJob]]
  def insertScrapeJob(sj: ScrapeJob): F[ScrapeJob]
  def updateScrapeJob(sj: ScrapeJob): F[ScrapeJob]
}
