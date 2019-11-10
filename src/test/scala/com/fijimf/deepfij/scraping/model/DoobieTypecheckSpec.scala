package com.fijimf.deepfij.scraping.model

import java.time.LocalDateTime

class DoobieTypecheckSpec extends DbIntegrationSpec {
  val containerName = "doobie-typecheck-spec"
  val port="17374"

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
