package com.fijimf.deepfij.scraping.model

import java.time.LocalDateTime

import cats.Applicative
import cats.effect.Sync
import doobie.implicits._
import doobie.util.update.Update0
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}

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


  implicit val scrapeRequestEncoder: Encoder.AsObject[ScrapeRequest] = deriveEncoder[ScrapeRequest]
  implicit val scrapeRequestDecoder: Decoder[ScrapeRequest] = deriveDecoder[ScrapeRequest]
  implicit def scrapeRequestEntityEncoder[F[_] : Applicative]: EntityEncoder[F, ScrapeRequest] = jsonEncoderOf
  implicit def scrapeRequestEntityDecoder[F[_] : Sync]: EntityDecoder[F, ScrapeRequest] = jsonOf

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

    def findByLatestScrape(season:Int, model:String): doobie.Query0[ScrapeRequest] = (baseQuery ++
      fr"""WHERE STATUS_CODE = 200 AND JOB_ID = (
             SELECT id
             FROM scrape_job
             WHERE completed_at = (
               SELECT MAX(completed_at) FROM scrape_job WHERE season = $season AND model = $model
             )
          )
    """).query[ScrapeRequest]

    def list(): doobie.Query0[ScrapeRequest] = baseQuery.query[ScrapeRequest]

    def delete(id: Long): doobie.Update0 = sql"DELETE FROM scrape_request where id=$id".update

  }

}
