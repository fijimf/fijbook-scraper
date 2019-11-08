package com.fijimf.deepfij.scraping.services

import com.fijimf.deepfij.scraping.model.ScrapeJob

trait AbstractScrapingRepo[F[_]] {
  def listScrapeJobs():F[List[ScrapeJob]]
}
