package com.fijimf.deepfij.scraping

import cats.data.NonEmptyList
import cats.effect.IO
import com.fijimf.deepfij.scraping.model.ScrapeJob
import com.fijimf.deepfij.scraping.model.ScrapeJob.JobsFilter
import org.http4s.{Method, Request, Uri}
import org.scalatest.FunSpec

class ScrapingRoutesSpec extends FunSpec {
  describe("ScrapingRoutes") {
    it("Should create correct job filter for query string") {
      assert(
        ScrapingRoutes.jobsFilterFromReq(
          Request[IO](
            Method.GET,
            Uri.uri("/jobs"))
        ) === JobsFilter(None, None, None)
      )
      assert(
        ScrapingRoutes.jobsFilterFromReq(
          Request[IO](
            Method.GET,
            Uri.uri("/jobs?season=2019&season=2020"))
        ) === JobsFilter(Some(NonEmptyList.of(2019,2020)), None, None)
      )
      assert(
        ScrapingRoutes.jobsFilterFromReq(
          Request[IO](
            Method.GET,
            Uri.uri("/jobs?model=Casablanca"))
        ) === JobsFilter(None, Some("Casablanca"), None)
      )
      assert(
        ScrapingRoutes.jobsFilterFromReq(
          Request[IO](
            Method.GET,
            Uri.uri("/jobs?model=Casablanca&completed"))
        ) === JobsFilter(None, Some("Casablanca"), Some(true))
      )
      assert(
        ScrapingRoutes.jobsFilterFromReq(
          Request[IO](
            Method.GET,
            Uri.uri("/jobs?model=Casablanca&completed=false"))
        ) === JobsFilter(None, Some("Casablanca"), Some(false))
      )
    }
  }
}
