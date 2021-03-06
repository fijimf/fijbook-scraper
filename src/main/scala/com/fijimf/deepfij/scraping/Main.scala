package com.fijimf.deepfij.scraping

import java.sql.{Connection, DriverManager}
import java.time.LocalDateTime

import cats.effect._
import cats.implicits._
import com.fijimf.deepfij.scraping.model.JobScheduler
import com.fijimf.deepfij.scraping.services.{Scraper, ScrapingRepo}
import com.fijimf.deepfij.scraping.util.ConfigUtils
import com.typesafe.config.{Config, ConfigFactory}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends IOApp {

  val config: IO[Config] = IO.delay(ConfigFactory.load())

  val resourceConf: Resource[IO, Config] = {
    val free: Config => IO[Unit] = (c: Config) => IO {}
    Resource.make(config)(free)
  }

  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      cf <- resourceConf
      driver: String = cf.getString("fijbook.scraping.db.driver")
      url: String = cf.getString("fijbook.scraping.db.url")
      user: String = cf.getString("fijbook.scraping.db.user")
      password: String = cf.getString("fijbook.scraping.db.password")
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      te <- ExecutionContexts.cachedThreadPool[IO]
      _ <- readyCheck(driver,url,user,password)
      xa <- HikariTransactor.newHikariTransactor[IO](driver, url, user, password, ce, te)
    } yield xa

  val client: Resource[IO, Client[IO]] =BlazeClientBuilder[IO](global).resource

  def run(args: List[String]): IO[ExitCode] = {
    transactor.use { xa =>
      client.use { c =>
        val repo: ScrapingRepo[IO] = ScrapingRepo[IO](xa)


        for {
          _ <- initDB(xa)
          port <- config.map(_.getInt("fijbook.scraping.port"))
          schedHost <- config.map(_.getString("fijbook.scraping.schedule.host"))
          schedPort <- config.map(_.getInt("fijbook.scraping.schedule.port"))
          scrapers <- config.map(ConfigUtils.loadScrapers)
          scraper: Scraper[IO] = Scraper(c, scrapers, repo, schedHost, schedPort)
          jobs <- config.map(ConfigUtils.loadJobs)
          fibers<- JobScheduler[IO]().scheduleMany(jobs, scraper, LocalDateTime.now())
          exitCode <- ScrapingServer
            .stream[IO](xa, scraper, repo, port, c, schedHost, schedPort, scrapers)
            .compile[IO, IO, ExitCode]
            .drain
            .as(ExitCode.Success)
        } yield {
          exitCode
        }
      }
    }
  }

  def readyCheck(driver:String, url:String, user:String, password:String): Resource[IO,Connection] = {
    val ioa = IO {
      Class.forName(driver)
      DriverManager.getConnection(url, user, password)
    }
    val alloc: IO[Connection] =retryWithBackOff(ioa,100.milliseconds,5)
    val free: Connection=>IO[Unit]=(c:Connection)=>IO{c.close()}
    Resource.make(alloc)(free)
  }

  def retryWithBackOff[A](ioa: IO[A], initialDelay: FiniteDuration, maxRetries: Int)
                         (implicit timer: Timer[IO]): IO[A] = {
    IO.delay(println(s"Checking database connection. $maxRetries retries left. "))
    ioa.handleErrorWith { error =>
      if (maxRetries > 0)
        IO.sleep(initialDelay) *> retryWithBackOff(ioa, initialDelay * 2, maxRetries - 1)
      else
        IO.raiseError(error)
    }
  }


  def initDB(xa: HikariTransactor[IO]): IO[Int] = {
    xa.configure { dataSource =>
      IO {
        Flyway
          .configure()
          .dataSource(dataSource)
          .locations("classpath:db-scraping/migration")
          .baselineOnMigrate(true)
          .table("flyway_schema_history_scraping")
          .load()
          .migrate()
      }
    }
  }
}
