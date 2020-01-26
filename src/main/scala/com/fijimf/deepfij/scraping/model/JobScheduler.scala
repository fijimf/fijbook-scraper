package com.fijimf.deepfij.scraping.model

import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import com.fijimf.deepfij.scraping.services.Scraper
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case class JobScheduler[F[_]]()(implicit c: Concurrent[F], cs: ContextShift[F], clock: Clock[F], tim: Timer[F]) {
  val log: Logger = LoggerFactory.getLogger(classOf[JobScheduler[F]])
  def schedule(scheduledJob: ScheduledJob, s:Scraper[F], time:LocalDateTime=LocalDateTime.now)(implicit c: Concurrent[F], cs: ContextShift[F], clock: Clock[F], tim: Timer[F]): F[Fiber[F, _]] = {
    for {
      _ <- c.delay(log.info(s"Scheduling job: ${scheduledJob.season} - ${scheduledJob.flavor}:  ${scheduledJob.cronEntry}"))
      thr <- repeat(scheduledJob, s, time).start
    } yield {
      thr
    }
  }

  def scheduleMany(scheduledJobs: List[ScheduledJob], scraper: Scraper[F], time:LocalDateTime): F[List[Fiber[F, _]]] = {
    scheduledJobs.map(schedule(_, scraper, time)).sequence
  }

  def repeat(scheduledJob: ScheduledJob, s:Scraper[F], time:LocalDateTime): F[_] = {
    val millisToSleep: F[Long] = clock.realTime(MILLISECONDS).map(
      secs=>{
        scheduledJob.nextIntervalMillis(millisToLocalDateTime(secs))
      }
    )
    val value: F[ScrapeJob] = if (scheduledJob.flavor === "update") {
      s.update(scheduledJob.season, LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
    } else {
      s.fill(scheduledJob.season)
    }
    for {
      _<-c.delay(log.info(s"Scheduling ${scheduledJob.season}/${scheduledJob.flavor}: ${scheduledJob.cronEntry}"))
      t<-millisToSleep
      _<-c.delay(log.info(s"${scheduledJob.season}/${scheduledJob.flavor} Millis to wait for is $t"))
      _<-tim.sleep(t.millis)
      _<-c.delay(log.info(s"${scheduledJob.season}/${scheduledJob.flavor} Done waiting"))
      _<-value >> repeat(scheduledJob, s, time)
    } yield {
    }
  }

  private def millisToLocalDateTime(millis: Long): LocalDateTime = {
    Try {
      LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    } match {
      case Failure(thr) =>
        log.error(s"Exception converting $millis to LocalDateTime.  Returning LocalDateTime.now() ad continuing", thr)
        LocalDateTime.now()
      case Success(ldt)=>ldt
    }
  }
}
