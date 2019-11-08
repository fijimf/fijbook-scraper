package com.fijimf.deepfij.scraping.model

import java.time.LocalDateTime

import doobie.implicits._
import doobie.util.update.Update0

case class ScrapeRequest
(
  id: Long,
  scrapeJobId: Long,
  modelKey: String,
  requestedAt: LocalDateTime,
  statusCode: Int,
  digest: String,
  updatesProposed: Int,
  updatesAccepted: Int
)

object ScrapeRequest {

  object Dao extends AbstractDao {

    override def cols: Array[String] = Array("id", "job_id", "model_key", "requested_at", "status_code",
      "digest", "updates_proposed", "updates_accepted")

    override def tableName: String = "scrape_request"

    def insert(r: ScrapeRequest): Update0 =
      (fr"""INSERT INTO scrape_request(job_id, model_key, requested_at, status_code, digest, updates_proposed, updates_accepted)
            VALUES (${r.scrapeJobId},${r.modelKey},${r.requestedAt},${r.statusCode},${r.digest},${r.updatesProposed},${r.updatesAccepted})
            RETURNING """ ++ colFr).update

    def update(r: ScrapeRequest): Update0 =
      (fr"""UPDATE scrape_request SET job_id = ${r.scrapeJobId}, model_key = ${r.modelKey}, requested_at = ${r.requestedAt}, status_code = ${r.statusCode}, digest = ${r.digest}, updates_proposed = ${r.updatesProposed}, updates_accepted = ${r.updatesAccepted}
            WHERE id=${r.id}
            RETURNING """ ++ colFr).update

    def find(id: Long): doobie.Query0[ScrapeRequest] = (baseQuery ++ fr" WHERE id = $id").query[ScrapeRequest]

    def findByScrapeJob(scrapeJobId: Long): doobie.Query0[ScrapeRequest] = (baseQuery ++ fr" WHERE job_id = $scrapeJobId").query[ScrapeRequest]

    def list(): doobie.Query0[ScrapeRequest] = baseQuery.query[ScrapeRequest]

    def delete(id: Long): doobie.Update0 = sql"DELETE FROM scrape_request where id=$id".update

  }

}
