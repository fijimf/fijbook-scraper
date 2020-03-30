package com.fijimf.deepfij.scraping.model

import java.time.LocalDateTime

import cats.data.NonEmptyList
import com.fijimf.deepfij.scraping.model.ScrapeJob.JobsFilter

class DoobieTypecheckSpec extends DbIntegrationSpec {
  val containerName = "doobie-typecheck-spec"
  val port = "17374"

  describe("Doobie typechecking Dao's") {
    describe("ScrapeJob.Dao") {
      it("insert should typecheck") {
        check(ScrapeJob.Dao.insert(ScrapeJob(0L, "update", 2020, "casablanca", LocalDateTime.now(), None)))
      }

      it("list should typecheck") {
        check(ScrapeJob.Dao.list())
      }

      it("find should typecheck") {
        check(ScrapeJob.Dao.find(99L))
      }

      it("findBySeason should typecheck") {
        check(ScrapeJob.Dao.findBySeason(2020))
      }

      it("findByFilter should typecheck") {
        check(ScrapeJob.Dao.findByFilter(JobsFilter(None, None, None)))
        check(ScrapeJob.Dao.findByFilter(JobsFilter(Some(NonEmptyList.of(2017, 2018)), None, None)))
        check(ScrapeJob.Dao.findByFilter(JobsFilter(None, Some("Casablanca"), None)))
        check(ScrapeJob.Dao.findByFilter(JobsFilter(None, None, Some(true))))
        check(ScrapeJob.Dao.findByFilter(JobsFilter(None, Some("Casablanca"), Some(true))))
        check(ScrapeJob.Dao.findByFilter(JobsFilter(Some(NonEmptyList.of(2017, 2018)), None, Some(true))))
      }

      it("delete should typecheck") {
        check(ScrapeJob.Dao.delete(99L))
      }

      it("update should typecheck") {
        check(ScrapeJob.Dao.update(ScrapeJob(1L, "fill", 2020, "web1ncaa", LocalDateTime.now(), None)))
      }

      it("truncate should typecheck") {
        check(ScrapeJob.Dao.truncate())
      }
    }
    describe("ScrapeRequest.Dao") {
      it("insert should typecheck") {
        check(ScrapeRequest.Dao.insert(ScrapeRequest(0L, 3L, "33", LocalDateTime.now(), 200, "jaDLlajdhljshdkjlD",15,0 )))
      }

      it("list should typecheck") {
        check(ScrapeRequest.Dao.list())
      }

      it("find should typecheck") {
        check(ScrapeRequest.Dao.find(99L))
      }

      it("findBySeason should typecheck") {
        check(ScrapeRequest.Dao.findByScrapeJob(3L))
      }

      it("delete should typecheck") {
        check(ScrapeRequest.Dao.delete(99L))
      }

      it("update should typecheck") {
        check(ScrapeRequest.Dao.update(ScrapeRequest(9L, 3L, "33", LocalDateTime.now(), 200, "jaDLlajdhljshdkjlD",15,0 )))
      }

      it("truncate should typecheck") {
        check(ScrapeRequest.Dao.truncate())
      }
    }
  }
}
