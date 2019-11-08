package com.fijimf.deepfij.scraping.model

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.schedule.model.{Alias, Conference, ConferenceMapping, Game, Result, Season, Team}

class DoobieTypecheckSpec extends DbIntegrationSpec {
  val containerName = "doobie-typecheck-spec"
  val port="17374"

  describe("Doobie typechecking Dao's") {
    describe("ScrapeJob.Dao") {
      it("insert should typecheck") {
        check(ScrapeJob.Dao.insert(ScrapeJob(0L, "update", 2020, LocalDateTime.now(),None )))
      }

      it("list should typecheck") {
        check(ScrapeJob.Dao.list())
      }

      it("find should typecheck") {
        check(ScrapeJob.Dao.find(99L))
      }

      it("findByLoadKey should typecheck") {
        check(ScrapeJob.Dao.findBySeason(2020))
      }

      it("delete should typecheck") {
        check(ScrapeJob.Dao.delete(99L))
      }

      it("update should typecheck") {
        check(ScrapeJob.Dao.update(ScrapeJob(1L, "update", 2020, LocalDateTime.now(),None )))
      }

      it("truncate should typecheck") {
        check(ScrapeJob.Dao.truncate())
      }
    }
  }
}
