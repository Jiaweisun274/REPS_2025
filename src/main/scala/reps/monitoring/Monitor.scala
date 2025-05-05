package reps.monitoring

// Monitor detects low output and drop alerts

import reps.models.Record
import reps.storage.CsvStorage

object Monitor {

  // Alert types
  case class LowOutputAlert(record: Record, threshold: Double)
  case class SuddenDropAlert(prev: Record, current: Record, dropPercent: Double)

  // CLI menu for alerts
  def runMonitoringMenu(dateTag: String): Unit = {
    println("\n--- Monitoring and Alerts ---")
    println("Select data source to monitor:")
    println(" 1) Solar")
    println(" 2) Wind")
    println(" 3) Hydro")
    println(" 4) Consumption")
    println(" 5) Back to main menu")
    print("Select> ")

    val label = scala.io.StdIn.readLine().trim match {
      case "1" => "solar"
      case "2" => "wind"
      case "3" => "hydro"
      case "4" => "consumption"
      case "5" => return
      case _   => println("Invalid selection. Returning to menu."); return
    }

    val records = CsvStorage.readRecords(s"$label-$dateTag.csv")
    if (records.isEmpty) {
      println("No records found for this dataset.")
      return
    }

    println("Choose check type:")
    println(" 1) Low output detection")
    println(" 2) Sudden drop detection")
    println(" 3) Back")
    print("Select> ")

    scala.io.StdIn.readLine().trim match {
      case "1" => runLowOutputCheck(records)
      case "2" => runSuddenDropCheck(records)
      case _   => println("Returning to menu.")
    }

    println("\nPress Enter to return to main menu...")
    scala.io.StdIn.readLine()
  }

  // Prompt threshold and check low output
  def runLowOutputCheck(records: Seq[Record]): Unit = {
    print("Enter output threshold (e.g., 1000 kWh): ")
    val threshold = scala.io.StdIn.readLine().trim.toDoubleOption.getOrElse(10.0)
    showLowOutputAlerts(records, threshold)
  }

  // Prompt threshold and check sudden drops
  def runSuddenDropCheck(records: Seq[Record]): Unit = {
    print("Enter sudden drop threshold percentage (e.g., 20): ")
    val threshold = scala.io.StdIn.readLine().trim.toDoubleOption.getOrElse(80.0)
    showSuddenDropAlerts(records, threshold)
  }

  // Find low output alerts
  def detectLowOutput(records: Seq[Record], threshold: Double): Seq[LowOutputAlert] = {
    records.filter(_.outputKWh < threshold).map(r => LowOutputAlert(r, threshold))
  }

  // Show low output
  def showLowOutputAlerts(records: Seq[Record], threshold: Double): Unit = {
    val alerts = detectLowOutput(records, threshold)
    if (alerts.isEmpty) {
      println("\n[✔] No low output alerts detected.")
    } else {
      println(f"\n--- Low Output Alerts (< $threshold%.2f kWh) ---")
      alerts.foreach { alert =>
        println(f"${alert.record.timestamp} | ${alert.record.source.id} | ${alert.record.outputKWh}%.2f kWh")
      }
      println(s"\n[!] ${alerts.size} alert(s) detected.")
    }
  }

  // Find sudden drops
  def detectSuddenDrops(records: Seq[Record], dropThresholdPercent: Double): Seq[SuddenDropAlert] = {
    val sorted = records.sortBy(_.timestamp.toEpochSecond)
    // Compare each pair for percentage drop
    sorted.sliding(2).collect {
      case Seq(prev, current) if prev.outputKWh > 0 =>
        val drop = ((prev.outputKWh - current.outputKWh) / prev.outputKWh) * 100
        if (drop >= dropThresholdPercent)
          Some(SuddenDropAlert(prev, current, drop))
        else None
    }.flatten.toSeq
  }

  // Show drop alerts
  def showSuddenDropAlerts(records: Seq[Record], dropThresholdPercent: Double): Unit = {
    val alerts = detectSuddenDrops(records, dropThresholdPercent)
    if (alerts.isEmpty) {
      println("\n[✔] No sudden drop alerts detected.")
    } else {
      println(f"\n--- Sudden Drop Alerts (>${dropThresholdPercent}%% drop) ---")
      alerts.foreach { alert =>
        println(f"From ${alert.prev.timestamp} (${alert.prev.outputKWh}%.2f kWh) to ${alert.current.timestamp} (${alert.current.outputKWh}%.2f kWh) | Drop: ${alert.dropPercent}%.2f%%")
      }
      println(s"\n[!] ${alerts.size} alert(s) detected.")
    }
  }
}