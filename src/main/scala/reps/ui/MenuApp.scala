package reps.ui

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
  private val apiKey  = "ab159c9a089a4a43882b487f0c2d0390"
  private val isoFmt  = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  private val dateFmt = DateTimeFormatter.ISO_DATE
  private val maxAtt  = 5
  private val backend = HttpURLConnectionBackend()

  def main(args: Array[String]): Unit = {
    val dataDir = java.nio.file.Paths.get("data")
    java.nio.file.Files.createDirectories(dataDir)

    var continueApp = true
    while (continueApp) {
      val (from, to, dateTag) = Downloader.promptDownloadRange()
      Seq((267, "solar"), (181, "wind"), (191, "hydro"), (124, "consumption")).foreach {
        case (id, label) => Downloader.downloadForDate(id, label, from, to, dateTag, dataDir.toString)
      }

      var inMenu = true
      while (inMenu) {
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
          case "1" => Viewer.viewData(dateTag)
          case "2" => DataAnalyzer.analyzeWithUserInput(dateTag, from, to)
          case "3" => Monitor.runMonitoringMenu(dateTag)  // 👈 Delegated to Monitor.scala
          case "4" => inMenu = false; continueApp = false
          case "5" => inMenu = false
          case _   => println("Invalid choice, try again.")
        }
      }
    }
    println("Goodbye!")
  }
}
