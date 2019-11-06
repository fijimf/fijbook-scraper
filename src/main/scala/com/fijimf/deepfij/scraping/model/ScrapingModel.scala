package com.fijimf.deepfij.scraping.model

import java.time.LocalDate

import com.fijimf.deepfij.schedule.model.UpdateCandidate

trait ScrapingModel[T] {

  val modelName:String

  val season:Int

  def keys:List[T]

  def urlFromKey(k:T):String

  def scrape(k:String, data:String):List[UpdateCandidate]

  def modelKey(k: T): String
}


