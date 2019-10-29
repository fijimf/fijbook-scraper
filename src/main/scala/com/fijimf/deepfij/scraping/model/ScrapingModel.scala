package com.fijimf.deepfij.scraping.model

import java.time.LocalDate

trait ScrapingModel[T] {

  val modelName:String

  val season:Int

  def keys:List[T]

  def urlFromKey(k:T):String



}
