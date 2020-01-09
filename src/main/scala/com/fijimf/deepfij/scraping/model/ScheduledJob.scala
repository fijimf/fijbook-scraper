package com.fijimf.deepfij.scraping.model

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import cats.effect.{Clock, Concurrent, ContextShift, Timer}
import cats.implicits._
import com.fijimf.deepfij.scraping.services.Scraper
import it.sauronsoftware.cron4j.{Predictor, SchedulingPattern}
import org.slf4j.{Logger, LoggerFactory}

case class ScheduledJob(season:Int, cronEntry:String, flavor:String) {
  require(SchedulingPattern.validate(cronEntry))
  private def localDateTimeToUtilDate(d:LocalDateTime): Date =Date.from(d.atZone(ZoneId.systemDefault()).toInstant)
  private def utilDateToLocalDateTime(u:Date): LocalDateTime =LocalDateTime.ofInstant(u.toInstant, ZoneId.systemDefault)

  val pattern = new SchedulingPattern(cronEntry)
  def nextMatchingTime(now:LocalDateTime=LocalDateTime.now): LocalDateTime = utilDateToLocalDateTime(new Predictor(pattern, localDateTimeToUtilDate(now)).nextMatchingDate())
  def nextIntervalMillis(now:LocalDateTime=LocalDateTime.now):Long =ChronoUnit.MILLIS.between(now, nextMatchingTime(now))


}

object ScheduledJob {
   import scala.concurrent.duration._
   def schedule[F[_]](job:ScheduledJob, scraper:Scraper[F])(implicit F: Concurrent[F], cs: ContextShift[F], clock: Clock[F], tim: Timer[F]):F[Unit]={
     for {
       _ <- tim.sleep(job.nextIntervalMillis(LocalDateTime.now).millis)
       _ <- if (job.flavor == "update") {
         scraper.update(job.season, LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
       } else {
         scraper.fill(job.season)
       }
       _ <- F.delay(schedule(job, scraper))
     } yield {
       ()
     }
   }

  def schedule[F[_]](jobs: List[ScheduledJob], scraper: Scraper[F])(implicit F: Concurrent[F], cs: ContextShift[F], clock: Clock[F], tim: Timer[F]): F[List[Unit]] = {
    jobs.map(schedule(_, scraper)).sequence
  }
}
