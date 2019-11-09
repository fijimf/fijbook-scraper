package com.fijimf.deepfij.scraping.services

import com.fijimf.deepfij.scraping.model.ScrapeJob

trait AbstractScrapingRepo[F[_]] {
  def listScrapeJobs():F[List[ScrapeJob]]
  def insertScrapeJob(sj: ScrapeJob): F[ScrapeJob]
}
