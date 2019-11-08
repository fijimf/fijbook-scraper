package com.fijimf.deepfij.scraping.services

import cats.effect.Sync
import com.fijimf.deepfij.scraping.model.ScrapeJob
import doobie.util.transactor.Transactor
import doobie.implicits._

class ScrapingRepo[F[_] : Sync](xa: Transactor[F]) extends AbstractScrapingRepo [F]{
   def listScrapeJobs():F[List[ScrapeJob]] =ScrapeJob.Dao.list().to[List].transact(xa)
}
