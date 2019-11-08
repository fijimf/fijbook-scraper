package com.fijimf.deepfij.scraping.model

import java.sql.Timestamp
import java.time.LocalDateTime

import doobie.implicits._
import doobie.util.Meta
import doobie.util.fragment.Fragment
import doobie.util.update.Update0

trait AbstractDao {
  implicit val localDateTimeMeta: Meta[LocalDateTime] = Meta[Timestamp].imap(ts => ts.toLocalDateTime)(ldt => Timestamp.valueOf(ldt))

  def cols: Array[String]
  def tableName:String

  def colString: String = cols.mkString(", ")

  def colFr: Fragment = Fragment.const(colString)
  def baseQuery: Fragment = fr"""SELECT """ ++ Fragment.const(colString) ++ fr""" FROM """++ Fragment.const(tableName+" ")

  def prefixedCols(p:String): Array[String] = cols.map(s=>p+"."+s)
  def prefixedQuery(p:String): Fragment = fr"""SELECT """ ++ Fragment.const(prefixedCols(p).mkString(",")) ++ fr""" FROM """++ Fragment.const(tableName+" "+p)

  def truncate(): Update0 = (fr"TRUNCATE "  ++ Fragment.const(tableName)++fr" CASCADE").update
}
