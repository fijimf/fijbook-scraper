package com.fijimf.deepfij.scraping.services

import cats.MonadError
import cats.effect.Sync
import com.fijimf.deepfij.scraping.model.{ScrapeJob, ScrapeRequest}
import doobie.implicits._
import doobie.util.transactor.Transactor

case class ScrapingRepo[F[_] : Sync](xa: Transactor[F]) extends AbstractScrapingRepo [F]{
   val me: MonadError[F, Throwable] = implicitly[MonadError[F, Throwable]]


   def listScrapeJobs():F[List[ScrapeJob]] =ScrapeJob.Dao.list().to[List].transact(xa)

   def insertScrapeJob(sj: ScrapeJob): F[ScrapeJob] = ScrapeJob.Dao.insert(sj)
     .withUniqueGeneratedKeys[ScrapeJob](ScrapeJob.Dao.cols: _*)
     .transact(xa).exceptSql(ex => me.raiseError[ScrapeJob](ex))
   def updateScrapeJob(sj: ScrapeJob): F[ScrapeJob] = ScrapeJob.Dao.update(sj)
     .withUniqueGeneratedKeys[ScrapeJob](ScrapeJob.Dao.cols: _*)
     .transact(xa).exceptSql(ex => me.raiseError[ScrapeJob](ex))
   def insertScrapeRequest(sr: ScrapeRequest): F[ScrapeRequest] = ScrapeRequest.Dao.insert(sr)
     .withUniqueGeneratedKeys[ScrapeRequest](ScrapeRequest.Dao.cols: _*)
     .transact(xa).exceptSql(ex => me.raiseError[ScrapeRequest](ex))
   def findScrapeRequestByLatestJob(season:Int, model:String): F[List[ScrapeRequest]] =
      ScrapeRequest.Dao.findByLatestScrape(season, model).to[List].transact(xa)
}
