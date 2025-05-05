package reps.download

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.nio.file.{Files, Paths}
import sttp.client3._
import sttp.client3.circe._
import sttp.model.StatusCode
import io.circe.generic.auto._
import reps.ui.CsvWrapper

// Downloads and saves Fingrid data
object Downloader {
  private val isoFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  private val dateFmt = DateTimeFormatter.ISO_DATE
  private val backend = HttpURLConnectionBackend()
  private var apiKey: String = ""

  // Set API key
  def setApiKey(key: String): Unit = {
    apiKey = key
  }
  private val maxAtt = 5

  // Ask user for time range
  def promptDownloadRange(): (ZonedDateTime, ZonedDateTime, String) = {
    println("=== REP System Data Downloader ===")
    println("Select download mode:")
    println(" 1) Single day")
    println(" 2) Custom range")
    println(" 3) Predefined intervals (last 24h,7d,30d,180d)")

    val validModes = Set("1", "2", "3")
    var mode = ""
    var valid = false
    while (!valid) {
      print("Choice> ")
      mode = scala.io.StdIn.readLine().trim
      // Validate input
      if (validModes.contains(mode)) valid = true
      else println("[Error] Invalid input. Please enter 1, 2, or 3.")
    }

    val (from, to) = mode match {
      case "1" =>
        val date = promptForDate("Enter date (YYYY-MM-DD): ")
        (date.atStartOfDay(ZoneOffset.UTC), date.plusDays(1).atStartOfDay(ZoneOffset.UTC))
      case "2" =>
        val start = promptForDate("Enter start date (YYYY-MM-DD): ")
        val end   = promptForDate("Enter end   date (YYYY-MM-DD): ")
        (start.atStartOfDay(ZoneOffset.UTC), end.plusDays(1).atStartOfDay(ZoneOffset.UTC))
      case "3" =>
        println(" a) Last 24 hours\n b) Last 7 days\n c) Last 30 days\n d) Last 180 days")
        print("Choice> ")
        scala.io.StdIn.readLine().trim.toLowerCase match {
          case "a" => val now = ZonedDateTime.now(ZoneOffset.UTC); (now.minusHours(24), now)
          case "b" => val now = ZonedDateTime.now(ZoneOffset.UTC); (now.minusDays(7), now)
          case "c" => val now = ZonedDateTime.now(ZoneOffset.UTC); (now.minusDays(30), now)
          case "d" => val now = ZonedDateTime.now(ZoneOffset.UTC); (now.minusDays(180), now)
          case _   => println("Invalid, defaulting to last 24 hours."); val now = ZonedDateTime.now(ZoneOffset.UTC); (now.minusHours(24), now)
        }
    }

    val dateTag = if (ChronoUnit.DAYS.between(from, to) == 1) from.toLocalDate.toString
    else s"${from.toLocalDate}_to_${to.toLocalDate}"
    (from, to, dateTag)
  }

  // Read and parse date
  private def promptForDate(prompt: String): LocalDate = {
    var parsed = false
    var date: LocalDate = null
    while (!parsed) {
      print(prompt)
      val input = scala.io.StdIn.readLine().trim
      try {
        date = LocalDate.parse(input, dateFmt)
        parsed = true
      } catch {
        case _: Exception => println("[Error] Invalid date format. Please use YYYY-MM-DD.")
      }
    }
    date
  }

  // Fetch and write data
  def downloadForDate(
                       datasetId: Int,
                       label: String,
                       from: ZonedDateTime,
                       to: ZonedDateTime,
                       dateTag: String,
                       dirPath: String
                     ): Unit = {
    // Abort if key missing
    if (apiKey.isEmpty) {
      println("[Error] API key not set. Please set the API key before downloading.")
      return
    }
    val isoFrom = isoFmt.format(from)
    val isoTo   = isoFmt.format(to)

    val dataCollected = new StringBuilder
    var page = 1
    var attempts = 0
    var moreData = true

    while (moreData && attempts < maxAtt) {
      // Fetch paginated data
      val pagedUri = uri"https://data.fingrid.fi/api/datasets/$datasetId/data"
        .addParam("startTime", isoFrom)
        .addParam("endTime", isoTo)
        .addParam("format", "csv")
        .addParam("oneRowPerTimePeriod", "true")
        .addParam("page", page.toString)
        .addParam("pageSize", "20000")

      val response = basicRequest
        .get(pagedUri)
        .header("x-api-key", apiKey)
        .response(asJson[CsvWrapper])
        .send(backend)

      if (response.code == StatusCode.TooManyRequests) {
        Thread.sleep(1000)
      } else {
        attempts += 1
        response.body match {
          // Append page data
          case Right(wrapper) =>
            val lines = wrapper.data.linesIterator.toList
            if (lines.size <= 1) moreData = false
            else {
              if (page == 1) dataCollected.append(wrapper.data)
              else dataCollected.append("\n" + lines.tail.mkString("\n"))
              page += 1
            }
          case Left(_) =>
            moreData = false
            println(s"Failed to retrieve data for page $page of $label.")
        }
      }
    }

    val rawCsv = dataCollected.toString
    if (rawCsv.isEmpty || !rawCsv.contains(";")) {
      println(s"[Warning] No data found for $label on $dateTag.")
    } else {
      val lines = rawCsv.linesIterator.toList
      val header = lines.head
      val dataLines = lines.tail

      val hourlyLines = dataLines
        .map { line =>
          val cols = line.split(";").map(_.replaceAll("\"", ""))
          val ts = ZonedDateTime.parse(cols(0), isoFmt)
          (line, ts)
        }
        .groupBy { case (_, ts) => ts.truncatedTo(ChronoUnit.HOURS) }
        .toSeq
        .sortBy(_._1)
        .map(_._2.head._1)

      if (hourlyLines.isEmpty) {
        println(s"[Info] Skipped $label-$dateTag.csv (no hourly data found)")
      } else {
        // Save file
        val filename = s"$label-$dateTag.csv"
        val outputPath = Paths.get(dirPath, filename)
        Files.write(outputPath, (header +: hourlyLines).mkString("\n").getBytes("UTF-8"))
        println(s"Wrote $outputPath")
      }
    }
  }
}
