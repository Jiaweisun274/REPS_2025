package reps.storage

import reps.models._
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.util.{Try, Success, Failure}

object CsvStorage {
  private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  
  def readRecords(path: String): Seq[Record] = {
    val file = new File(path)
    if (!file.exists()) {
      println(s"[WARN] CSV file not found: $path")
      return Seq.empty
    }
    
    val prefix = file.getName.takeWhile(c => c != '-' && c != '_').toLowerCase
    val sourceId = s"FG-${prefix.toUpperCase}"

    scala.io.Source.fromFile(file).getLines().drop(1).flatMap { raw =>
      val cols = raw.trim
        .stripPrefix("\"").stripSuffix("\"")
        .split(";")
        .map(_.trim.stripPrefix("\"").stripSuffix("\""))

      if (cols.length < 3) {
        println(s"[ERROR] Invalid line in $path: $raw")
        None
      } else {
        Try {
          val timestamp = ZonedDateTime.parse(cols(0), isoFormatter)
          val output = cols(2).toDouble
          Record(timestamp, Source(sourceId), output)
        } match {
          case Success(record) => Some(record)
          case Failure(ex) =>
            println(s"[ERROR] Failed to parse $path line: $raw → ${ex.getMessage}")
            None
        }
      }
    }.toSeq
  }
}