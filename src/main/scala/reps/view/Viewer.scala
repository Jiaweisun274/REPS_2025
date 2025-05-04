package reps.view

import reps.storage.CsvStorage
import scala.io.StdIn

object Viewer {

  def viewData(dateTag: String): Unit = {
    var viewing = true
    while (viewing) {
      println(s"\n--- View Data for $dateTag ---")
      println(" 1) Solar")
      println(" 2) Wind")
      println(" 3) Hydro")
      println(" 4) Consumption")
      println(" 5) Back to main menu")
      print("Select> ")

      StdIn.readLine().trim match {
        case "1" => printRecords("solar", dateTag)
        case "2" => printRecords("wind", dateTag)
        case "3" => printRecords("hydro", dateTag)
        case "4" => printRecords("consumption", dateTag)
        case "5" => viewing = false
        case _   => println("Invalid choice, try again.")
      }
    }
  }

  private def printRecords(label: String, dateTag: String): Unit = {
    println(s"\n### $label ###")
    val recs = CsvStorage.readRecords(s"data/$label-$dateTag.csv")
    if (recs.isEmpty) println("  (no records)")
    else recs.foreach(r => println(f"${r.timestamp} | ${r.source.id}%-12s | ${r.outputKWh}%8.2f kWh"))
  }
} 
