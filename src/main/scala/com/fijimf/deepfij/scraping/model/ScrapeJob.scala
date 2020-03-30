package com.fijimf.deepfij.scraping.model

import java.time.LocalDateTime

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.Sync
import doobie.implicits._
import doobie.util.update.Update0
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

case class ScrapeJob(id: Long, updateOrFill: String, season: Int, model:String, startedAt: LocalDateTime, completedAt: Option[LocalDateTime]) {

}

object ScrapeJob {

  implicit val scrapeJobEncoder: Encoder.AsObject[ScrapeJob] = deriveEncoder[ScrapeJob]
  implicit val scrapeJobDecoder: Decoder[ScrapeJob] = deriveDecoder[ScrapeJob]
  implicit def scrapeJobEntityEncoder[F[_] : Applicative]: EntityEncoder[F, ScrapeJob] = jsonEncoderOf
  implicit def scrapeJobEntityDecoder[F[_] : Sync]: EntityDecoder[F, ScrapeJob] = jsonOf
  implicit val scrapeListJobEncoder: Encoder.AsObject[List[ScrapeJob]] = deriveEncoder[List[ScrapeJob]]
  implicit val scrapeListJobDecoder: Decoder[List[ScrapeJob]] = deriveDecoder[List[ScrapeJob]]
  implicit def scrapeListJobEntityEncoder[F[_] : Applicative]: EntityEncoder[F, List[ScrapeJob]] = jsonEncoderOf
  implicit def scrapeListJobEntityDecoder[F[_] : Sync]: EntityDecoder[F, List[ScrapeJob]] = jsonOf

  case class JobsFilter(years:Option[NonEmptyList[Int]], model:Option[String], completed:Option[Boolean])

  object Dao extends AbstractDao {

    override def cols: Array[String] = Array("id", "update_or_fill", "season", "model", "started_at", "completed_at")

    override def tableName: String = "scrape_job"

    def insert(sj: ScrapeJob): Update0 =
      (fr"""INSERT INTO scrape_job(update_or_fill, season, model, started_at, completed_at )
            VALUES (${sj.updateOrFill},${sj.season}, ${sj.model},  ${sj.startedAt},${sj.completedAt})
            RETURNING """ ++ colFr).update

    def update(sj: ScrapeJob): Update0 =
      (fr"""UPDATE scrape_job SET update_or_fill = ${sj.updateOrFill}, season = ${sj.season}, model= ${sj.model}, started_at = ${sj.startedAt}, completed_at = ${sj.completedAt}
            WHERE id=${sj.id}
            RETURNING """ ++ colFr).update

    def find(id: Long): doobie.Query0[ScrapeJob] = (baseQuery ++ fr" WHERE id = $id").query[ScrapeJob]

    def findBySeason(season: Int): doobie.Query0[ScrapeJob] = (baseQuery ++ fr" WHERE season = $season").query[ScrapeJob]

    def findByFilter(filter: JobsFilter): doobie.Query0[ScrapeJob] = {
      import doobie.util.fragments._
      (baseQuery ++ whereAndOpt(
        filter.model.map(m => fr" model = $m"),
        filter.years.map(ys => in(fr" season",ys)),
        filter.completed.map(p => if (p) fr" completed_at is not null" else fr" completed_at is null")
      )).query[ScrapeJob]
    }

    def list(): doobie.Query0[ScrapeJob] = baseQuery.query[ScrapeJob]

    def delete(id: Long): doobie.Update0 = sql"DELETE FROM scrape_job where id=$id".update

  }

}