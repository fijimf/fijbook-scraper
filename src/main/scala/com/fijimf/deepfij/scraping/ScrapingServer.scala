package com.fijimf.deepfij.scraping

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, Timer}
import cats.syntax.semigroupk._
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.syntax.kleisli._
import org.http4s.{HttpApp, HttpRoutes}


object ScrapingServer {

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  def stream[F[_] : ConcurrentEffect](transactor: Transactor[F])(implicit T: Timer[F], C: ContextShift[F]): Stream[F, ExitCode] = {
    val repo: ScheduleRepo[F] = new ScheduleRepo[F](transactor)
    val healthcheckService: HttpRoutes[F] = ScrapingRoutes.healthcheckRoutes(repo)
    val aliasRepoService: HttpRoutes[F] = AliasRoutes.routes(repo)
    val conferenceRepoService: HttpRoutes[F] = ConferenceRoutes.routes(repo)
    val conferenceMappingRepoService: HttpRoutes[F] = ConferenceMappingRoutes.routes(repo)
    val gameRepoService: HttpRoutes[F] = GameRoutes.routes(repo)
    val resultRepoService: HttpRoutes[F] = ResultRoutes.routes(repo)
    val seasonRepoService: HttpRoutes[F] = SeasonRoutes.routes(repo)
    val teamRepoService: HttpRoutes[F] = TeamRoutes.routes(repo)
    val scheduleService: HttpRoutes[F] = routes.ScheduleRoutes.routes(repo)
    val snapshotterService: HttpRoutes[F] = ScrapingRoutes.snapshotterRoutes(new Snapshotter[F](transactor))
    val updaterService: HttpRoutes[F] = ScrapingRoutes.updaterRoutes(new Updater[F](transactor))
    val httpApp: HttpApp[F] = (
      healthcheckService <+>
        aliasRepoService <+>
        conferenceRepoService <+>
        conferenceMappingRepoService <+>
        gameRepoService <+>
        resultRepoService <+>
        seasonRepoService <+>
        teamRepoService <+>
        scheduleService <+>
        snapshotterService <+>
        updaterService).orNotFound
    val finalHttpApp: HttpApp[F] = Logger.httpApp[F](logHeaders = true, logBody = true)(httpApp)
    for {
      exitCode <- BlazeServerBuilder[F]
        .bindHttp(port = 8073, host = "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield {
      exitCode
    }
    }.drain


}
