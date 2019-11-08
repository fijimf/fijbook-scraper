package com.fijimf.deepfij.scraping.model

import java.time.LocalDateTime

case class ScrapeRequest(id:Long, scrapeRequestId:Long, modelKey:String, requestedAt:LocalDateTime, statusCode:Int, digest:String, updatesProposed:Int, updatesAccepted:Int) {

}
