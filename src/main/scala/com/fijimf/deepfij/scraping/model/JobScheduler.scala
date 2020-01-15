package com.fijimf.deepfij.scraping.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import com.fijimf.deepfij.scraping.services.Scraper
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._

case class JobScheduler[F[_]]()(implicit c: Concurrent[F], cs: ContextShift[F], clock: Clock[F], tim: Timer[F]) {
  val log: Logger = LoggerFactory.getLogger(classOf[JobScheduler[F]])
  def schedule(scheduledJob: ScheduledJob, s:Scraper[F], time:LocalDateTime=LocalDateTime.now)(implicit c: Concurrent[F], cs: ContextShift[F], clock: Clock[F], tim: Timer[F]): F[Fiber[F, _]] = {
    log.error("Z22111ooœįZZZZZŽZZZZŽZghñZZZŽZZZZŽZZZZŽ")

    for {
      _ <- c.delay(log.info(s"${LocalDateTime.now} ${scheduledJob.season}-${scheduledJob.flavor}:  ${scheduledJob.cronEntry}"))
      thr <- repeat(scheduledJob, s, time).start
    } yield {
      thr
    }
  }

  def scheduleMany(scheduledJobs: List[ScheduledJob], scraper: Scraper[F],time:LocalDateTime=LocalDateTime.now): F[List[Fiber[F, _]]] = {
    log.error("ZZZZZZŽZZZZŽZZZZŽZZZZŽZZZZŽ")
    scheduledJobs.map(schedule(_, scraper, time)).sequence
  }

  def repeat(scheduledJob: ScheduledJob, s:Scraper[F], time:LocalDateTime): F[_] = {
    val millis: Long = scheduledJob.nextIntervalMillis(time)
    val value: F[ScrapeJob] = if (scheduledJob.flavor === "update") {
      s.update(scheduledJob.season, LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
    } else {
      s.fill(scheduledJob.season)
    }
    tim.sleep(millis.millis)  *> value >> repeat(scheduledJob, s, time)
  }
}
