package com.fijimf.deepfij.scraping.model

import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.update.Update0

trait AbstractDao {
  def cols: Array[String]
  def tableName:String

  def colString: String = cols.mkString(", ")

  def colFr: Fragment = Fragment.const(colString)
  def baseQuery: Fragment = fr"""SELECT """ ++ Fragment.const(colString) ++ fr""" FROM """++ Fragment.const(tableName+" ")

  def prefixedCols(p:String): Array[String] = cols.map(s=>p+"."+s)
  def prefixedQuery(p:String): Fragment = fr"""SELECT """ ++ Fragment.const(prefixedCols(p).mkString(",")) ++ fr""" FROM """++ Fragment.const(tableName+" "+p)

  def truncate(): Update0 = (fr"TRUNCATE "  ++ Fragment.const(tableName)++fr" CASCADE").update
}
