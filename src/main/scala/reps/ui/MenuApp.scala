package reps.ui

// MenuApp is the CLI entry point for the REP system

import sttp.client3._
import io.circe.generic.auto._
import reps.analysis.DataAnalyzer
import reps.monitoring.Monitor
import reps.download.Downloader
import reps.view.Viewer
import scala.io.StdIn
import java.time._
import java.time.format.DateTimeFormatter



case class CsvWrapper(data: String)

object MenuApp {
  // Prompt for API key
  private lazy val apiKey: String = {
    print("Enter Fingrid API Key: ")
    StdIn.readLine().trim
  }
  Downloader.setApiKey(apiKey)
  
  private val isoFmt  = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  private val dateFmt = DateTimeFormatter.ISO_DATE
  private val maxAtt  = 5
  private val backend = HttpURLConnectionBackend()

  def main(args: Array[String]): Unit = {
    // Create data directory
    val dataDir = java.nio.file.Paths.get("data")
    java.nio.file.Files.createDirectories(dataDir)

    runMainLoop(dataDir)
    println("Goodbye!")
  }

  @annotation.tailrec
  def runMainLoop(dataDir: java.nio.file.Path): Unit = {
    val (from, to, dateTag) = Downloader.promptDownloadRange()
    Seq((267, "solar"), (181, "wind"), (191, "hydro"), (124, "consumption")).foreach {
      case (id, label) => Downloader.downloadForDate(id, label, from, to, dateTag, dataDir.toString)
    }

    if (runMenu(dateTag, from, to)) runMainLoop(dataDir)
  }

  @annotation.tailrec
  def runMenu(dateTag: String, from: ZonedDateTime, to: ZonedDateTime): Boolean = {
    println(
      """
        |--- Main Menu ---
        |1) View data
        |2) Analyze data
        |3) Monitor and Alerts
        |4) Exit
        |5) Download new data
        |Select> """.stripMargin)
    StdIn.readLine().trim match {
      case "1" =>
        Viewer.viewData(dateTag)
        runMenu(dateTag, from, to)
      case "2" =>
        DataAnalyzer.analyzeWithUserInput(dateTag, from, to)
        runMenu(dateTag, from, to)
      case "3" =>
        Monitor.runMonitoringMenu(dateTag)
        runMenu(dateTag, from, to)
      case "4" =>
        false
      case "5" =>
        true
      case _ =>
        println("Invalid choice, try again.")
        runMenu(dateTag, from, to)
    }
  }
}
