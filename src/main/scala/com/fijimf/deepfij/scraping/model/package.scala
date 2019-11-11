package com.fijimf.deepfij.scraping

import cats.Applicative
import cats.effect.Sync
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}


package object model {
  implicit val scrapeJobEncoder: Encoder.AsObject[ScrapeJob] = deriveEncoder[ScrapeJob]
  implicit val scrapeJobDecoder: Decoder[ScrapeJob] = deriveDecoder[ScrapeJob]
  implicit def scrapeJobEntityEncoder[F[_] : Applicative]: EntityEncoder[F, ScrapeJob] = jsonEncoderOf
  implicit def scrapeJobEntityDecoder[F[_] : Sync]: EntityDecoder[F, ScrapeJob] = jsonOf
}
