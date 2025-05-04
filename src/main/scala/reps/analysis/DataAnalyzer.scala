package reps.analysis

import reps.storage.CsvStorage
import reps.models.Record
import scala.io.StdIn
import java.time.temporal.{ChronoUnit, WeekFields}
import java.time.{ZonedDateTime, LocalDate}

object DataAnalyzer {
  
  def groupByGranularity(records: Seq[Record], granularity: String): Map[String, Seq[Record]] = {
    import java.time.format.DateTimeFormatter
    import java.time.temporal.WeekFields
    val formatter = granularity.toLowerCase match {
      case "hourly" =>
        (r: Record) => r.timestamp.truncatedTo(ChronoUnit.HOURS).toString
      case "daily" =>
        (r: Record) => r.timestamp.toLocalDate.toString
      case "weekly" =>
        val weekFields = WeekFields.ISO
        (r: Record) => {
          val week = r.timestamp.toLocalDate.get(weekFields.weekOfWeekBasedYear())
          val year = r.timestamp.toLocalDate.getYear
          f"$year-W$week%02d"
        }
      case "monthly" =>
        (r: Record) => {
          val d = r.timestamp.toLocalDate
          f"${d.getYear}-${d.getMonthValue.formatted("%02d")}"
        }
      case _ =>
        (r: Record) => r.timestamp.toString
    }
    records.groupBy(formatter)
  }
  
  def analyzeWithUserInput(dateTag: String, from: ZonedDateTime, to: ZonedDateTime): Unit = {
    println(s"\n--- Data Analysis for $dateTag ---")

    println("Select data source:")
    println(" 1) Solar")
    println(" 2) Wind")
    println(" 3) Hydro")
    println(" 4) Consumption")
    print("Select> ")
    val source = StdIn.readLine().trim match {
      case "1" => "solar"
      case "2" => "wind"
      case "3" => "hydro"
      case "4" => "consumption"
      case _   => println("Invalid selection. Defaulting to 'solar'."); "solar"
    }

    val records = CsvStorage.readRecords(s"$source-$dateTag.csv")
    if (records.isEmpty) {
      println("No records found for this dataset.")
      return
    }

    val durationDays = ChronoUnit.DAYS.between(from, to)
    var granularity: String = ""
    var valid = false

    while (!valid) {
      println("Choose time granularity:")
      println(" 1) Hourly\n 2) Daily\n 3) Weekly\n 4) Monthly")
      print("Select> ")
      StdIn.readLine().trim match {
        case "1" => granularity = "hourly"; valid = true
        case "2" => granularity = "daily"; valid = true
        case "3" =>
          if (durationDays >= 7) { granularity = "weekly"; valid = true }
          else println("[Error] Weekly analysis requires at least 7 days of data.")
        case "4" =>
          if (durationDays >= 30) { granularity = "monthly"; valid = true }
          else println("[Error] Monthly analysis requires at least 30 days of data.")
        case _ => println("Invalid input, try again.")
      }
    }

    // Use DataAnalyzer.groupByGranularity and compute totals per group
    val totalsPerGroup = DataAnalyzer.groupByGranularity(records, granularity)
      .mapValues(_.map(_.outputKWh).sum)
      .values
      .toSeq

    val groupStats = DataAnalyzer.groupByGranularity(records, granularity).toSeq.sortBy(_._1).map { case (period, recs) =>
      val total = recs.map(_.outputKWh).sum
      (period, total)
    }

    println(s"\n--- Grouped $granularity Data ---")
    groupStats.foreach { case (period, total) =>
      println(f"$period: $total%.2f kWh")
    }

    println("\nStatistics:")
    println(f"Mean:     ${mean(totalsPerGroup)}%.2f kWh")
    println(f"Median:   ${median(totalsPerGroup)}%.2f kWh")
    println(s"Mode:     ${mode(totalsPerGroup).mkString(", ")}")
    println(f"Range:    ${range(totalsPerGroup)}%.2f kWh")
    println(f"Midrange: ${midrange(totalsPerGroup)}%.2f kWh")

    println("\nPress Enter to return to the main menu...")
    StdIn.readLine()
  }

  def mean(data: Seq[Double]): Double = if (data.isEmpty) 0.0 else data.sum / data.size

  def median(data: Seq[Double]): Double = {
    val sorted = data.sorted
    val size = sorted.size
    if (size == 0) 0.0
    else if (size % 2 == 1) sorted(size / 2)
    else (sorted(size / 2 - 1) + sorted(size / 2)) / 2
  }

  def mode(data: Seq[Double]): Seq[Double] = {
    if (data.isEmpty) return Seq.empty
    val grouped = data.groupBy(identity).view.mapValues(_.size)
    val maxFreq = grouped.values.max
    grouped.filter(_._2 == maxFreq).keys.toSeq.sorted
  }

  def range(data: Seq[Double]): Double = if (data.isEmpty) 0.0 else data.max - data.min

  def midrange(data: Seq[Double]): Double = if (data.isEmpty) 0.0 else (data.max + data.min) / 2
}