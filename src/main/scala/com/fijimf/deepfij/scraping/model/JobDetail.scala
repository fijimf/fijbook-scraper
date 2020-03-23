package com.fijimf.deepfij.scraping.model

import cats.Applicative
import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonOf, jsonEncoderOf}

case class JobDetail(scrapeJob: ScrapeJob, requests:List[ScrapeRequest]) {

}

object JobDetail {
  implicit val jobDetailEncoder: Encoder.AsObject[JobDetail] = deriveEncoder[JobDetail]
  implicit val jobDetailDecoder: Decoder[JobDetail] = deriveDecoder[JobDetail]
  implicit def jobDetailEntityEncoder[F[_] : Applicative]: EntityEncoder[F, JobDetail] = jsonEncoderOf
  implicit def jobDetailEntityDecoder[F[_] : Sync]: EntityDecoder[F, JobDetail] = jsonOf

}
