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
  val log = LoggerFactory.getLogger(classOf[ScheduledJob])
  require(SchedulingPattern.validate(cronEntry))
  private def localDateTimeToUtilDate(d:LocalDateTime): Date =Date.from(d.atZone(ZoneId.systemDefault()).toInstant)
  private def utilDateToLocalDateTime(u:Date): LocalDateTime =LocalDateTime.ofInstant(u.toInstant, ZoneId.systemDefault)

  val pattern = new SchedulingPattern(cronEntry)
  def nextMatchingTime(now:LocalDateTime=LocalDateTime.now): LocalDateTime = utilDateToLocalDateTime(new Predictor(pattern, localDateTimeToUtilDate(now)).nextMatchingDate())
  def nextIntervalMillis(now:LocalDateTime=LocalDateTime.now):Long ={
    val time: LocalDateTime = nextMatchingTime(now)
    val millis: Long = ChronoUnit.MILLIS.between(now, time)
    log.info(s"$time => $millis")
    millis
  }


}
