package com.fijimf.deepfij.scraping.model

import java.sql.Timestamp
import java.time.LocalDateTime

import doobie.implicits._
import doobie.util.Meta
import doobie.util.update.Update0

case class ScrapeJob(id: Long, updateOrFill: String, season: Int, startedAt: LocalDateTime, completedAt: Option[LocalDateTime]) {

}

object ScrapeJob {

  object Dao extends AbstractDao {
    implicit val localDateTimeMeta: Meta[LocalDateTime] = Meta[Timestamp].imap(ts => ts.toLocalDateTime)(ldt => Timestamp.valueOf(ldt))

    override def cols: Array[String] = Array("id", "update_or_fill", "season", "started_at", "completed_at")

    override def tableName: String = "scrape_job"

    def insert(sj: ScrapeJob): Update0 =
      (fr"""INSERT INTO scrape_job(update_or_fill, season, started_at, completed_at )
            VALUES (${sj.updateOrFill},${sj.season},${sj.startedAt},${sj.completedAt})
            RETURNING """ ++ colFr).update

    def update(sj: ScrapeJob): Update0 =
      (fr"""UPDATE scrape_job SET update_or_fill = ${sj.updateOrFill}, season = ${sj.season}, started_at = ${sj.startedAt}, completed_at = ${sj.completedAt}
            WHERE id=${sj.id}
            RETURNING """ ++ colFr).update

    def find(id: Long): doobie.Query0[ScrapeJob] = (baseQuery ++ fr" WHERE id = $id").query[ScrapeJob]

    def findBySeason(season: Int): doobie.Query0[ScrapeJob] = (baseQuery ++ fr" WHERE season = $season").query[ScrapeJob]

    def list(): doobie.Query0[ScrapeJob] = baseQuery.query[ScrapeJob]

    def delete(id: Long): doobie.Update0 = sql"DELETE FROM scrape_job where id=$id".update

  }

}