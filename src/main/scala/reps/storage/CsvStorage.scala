package reps.storage

// CsvStorage reads and parses local CSV files into Record objects

import reps.models._
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.util.{Try, Success, Failure}

object CsvStorage {
  private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  
  // Read records from a csv file
  def readRecords(path: String): Seq[Record] = {
    val file = new File(path)
    if (!file.exists()) {
      // File not found
      println(s"[WARN] CSV file not found: $path")
      return Seq.empty
    }
    
    // Infer source type from filename
    val prefix = file.getName.takeWhile(c => c != '-' && c != '_').toLowerCase
    val sourceId = s"FG-${prefix.toUpperCase}"

    scala.io.Source.fromFile(file).getLines().drop(1).flatMap { raw =>
      // Parse each line
      val cols = raw.trim
        .stripPrefix("\"").stripSuffix("\"")
        .split(";")
        .map(_.trim.stripPrefix("\"").stripSuffix("\""))

      if (cols.length < 3) {
        // Skip malformed rows
        println(s"[ERROR] Invalid line in $path: $raw")
        None
      } else {
        Try {
          // Extract and convert values
          val timestamp = ZonedDateTime.parse(cols(0), isoFormatter)
          val output = cols(2).toDouble
          Record(timestamp, Source(sourceId), output)
        } match {
          case Success(record) => Some(record)
          case Failure(ex) =>
            // Log parsing error
            println(s"[ERROR] Failed to parse $path line: $raw → ${ex.getMessage}")
            None
        }
      }
    }.toSeq
  }
}