package com.fijimf.deepfij.scraping.util

import cats.Applicative
import com.fijimf.deepfij.scraping.BuildInfo
import io.circe.Encoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

case class ServerInfo(name: String, version: String, scalaVersion: String, sbtVersion: String, buildNumber: Int, builtAt: String, status: Map[String, Boolean]){
  def isOk: Boolean = status.values.forall(identity)
}

case object ServerInfo {

  implicit val healthyEncoder: Encoder.AsObject[ServerInfo] =
    Encoder.forProduct7("name", "version", "scalaVersion", "sbtVersion", "buildNumber", "builtAt", "isOk")(u =>
      (u.name, u.version, u.scalaVersion, u.sbtVersion, u.buildNumber, u.builtAt, u.isOk)
    )

  implicit def healthyEntityEncoder[F[_] : Applicative]: EntityEncoder[F, ServerInfo] = jsonEncoderOf

  def fromStatus(status: Map[String, Boolean]): ServerInfo = {
    ServerInfo(
      BuildInfo.name,
      BuildInfo.version,
      BuildInfo.scalaVersion,
      BuildInfo.sbtVersion,
      BuildInfo.buildInfoBuildNumber,
      BuildInfo.builtAtString,
      status)
  }
}