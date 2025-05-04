package reps.ui

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout.{VBox, HBox}
import reps.download.Downloader
import reps.analysis.DataAnalyzer
import reps.monitoring.Monitor
import reps.storage.CsvStorage
import reps.models.Record

import scalafx.collections.CollectionIncludes._

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import java.nio.file.{Files, Paths}
import scala.util.Try
import scalafx.scene.chart.{NumberAxis, LineChart, XYChart}
import scalafx.collections.ObservableBuffer
import scalafx.scene.chart.CategoryAxis

object MainGUI extends JFXApp3 {

  private val dataDir = Paths.get("data")
  Files.createDirectories(dataDir)

  private var lastDateTag: String = ""
  private var fromDate: ZonedDateTime = _
  private var toDate: ZonedDateTime = _
  private val dateFmt = DateTimeFormatter.ISO_DATE

  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title = "REP GUI"
      scene = new Scene(700, 600) {

        val infoLabel = new Label("Choose an action:")

        val dateInput = new TextField {
          promptText = "Enter date (YYYY-MM-DD)"
        }

        val rangeStart = new TextField {
          promptText = "Start date (YYYY-MM-DD)"
        }

        val rangeEnd = new TextField {
          promptText = "End date (YYYY-MM-DD)"
        }

        val predefinedBox = new ComboBox(Seq(
          "Last 24 hours",
          "Last 7 days",
          "Last 30 days",
          "Last 180 days"
        )) {
          promptText = "Select predefined range"
        }

        val outputArea = new TextArea {
          editable = false
          prefRowCount = 20
        }

        // Chart placeholder and data model for chart
        val chartXAxis = new CategoryAxis() {
          tickLabelRotation = -45
          tickLabelGap = 5
          tickLabelFont = scalafx.scene.text.Font.font(9)
        }
        val chartYAxis = new NumberAxis() { label = "kWh Output" }
        val chartSeries = new XYChart.Series[String, Number]()
        val lineChart = new LineChart[String, Number](chartXAxis, chartYAxis) {
          title = "Energy Output Over Time"
          data() += chartSeries
          prefHeight = 300
        }

        def setDateRange(tag: String, from: ZonedDateTime, to: ZonedDateTime): Unit = {
          lastDateTag = tag
          fromDate = from
          toDate = to
        }

        val downloadBtn = new Button("Download") {
          onAction = _ => {
            try {
              val d = LocalDate.parse(dateInput.text.value.trim, dateFmt)
              val from = d.atStartOfDay(ZoneOffset.UTC)
              val to = from.plusDays(1)
              val tag = d.toString
              val sources = Seq((267,"solar"), (181,"wind"), (191,"hydro"), (124,"consumption"))
              sources.foreach { case (id, label) =>
                Downloader.downloadForDate(id, label, from, to, tag, dataDir.toString)
              }
              setDateRange(tag, from, to)
              outputArea.text = s"Downloaded data for $tag"
            } catch {
              case e: Exception => outputArea.text = s"Invalid date: ${e.getMessage}"
            }
          }
        }

        val downloadRangeBtn = new Button("Download Range") {
          onAction = _ => {
            try {
              val s = LocalDate.parse(rangeStart.text.value.trim, dateFmt)
              val e = LocalDate.parse(rangeEnd.text.value.trim, dateFmt)
              val from = s.atStartOfDay(ZoneOffset.UTC)
              val to = e.plusDays(1).atStartOfDay(ZoneOffset.UTC)
              val tag = s"${s}_to_${e}"
              val sources = Seq((267,"solar"), (181,"wind"), (191,"hydro"), (124,"consumption"))
              sources.foreach { case (id, label) =>
                Downloader.downloadForDate(id, label, from, to, tag, dataDir.toString)
              }
              setDateRange(tag, from, to)
              outputArea.text = s"Downloaded data for range $tag"
            } catch {
              case e: Exception => outputArea.text = s"Invalid date range: ${e.getMessage}"
            }
          }
        }

        val downloadPredefinedBtn = new Button("Download Predefined") {
          onAction = _ => {
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val (from, to, tag) = predefinedBox.value.value match {
              case "Last 24 hours" => (now.minusHours(24), now, now.minusHours(24).toLocalDate.toString + "_to_" + now.toLocalDate.toString)
              case "Last 7 days"   => (now.minusDays(7), now, now.minusDays(7).toLocalDate.toString + "_to_" + now.toLocalDate.toString)
              case "Last 30 days"  => (now.minusDays(30), now, now.minusDays(30).toLocalDate.toString + "_to_" + now.toLocalDate.toString)
              case "Last 180 days" => (now.minusDays(180), now, now.minusDays(180).toLocalDate.toString + "_to_" + now.toLocalDate.toString)
              case _ =>
                outputArea.text = "Please select a predefined range."
                (null,null)
            }

            val sources = Seq((267,"solar"), (181,"wind"), (191,"hydro"), (124,"consumption"))
            sources.foreach { case (id, label) =>
              Downloader.downloadForDate(id, label, from, to, tag, dataDir.toString)
            }
            setDateRange(tag, from, to)
            outputArea.text = s"Downloaded data for predefined range: $tag"
          }
        }

        val datasetBox = new ComboBox(Seq("solar", "wind", "hydro", "consumption")) {
          promptText = "Select dataset to view"
        }

        val analyzeBox = new ComboBox(Seq("All", "solar", "wind", "hydro", "consumption")) {
          promptText = "Select dataset to analyze"
        }

        val granularityBox = new ComboBox(Seq("Hourly", "Daily", "Weekly", "Monthly")) {
          promptText = "Select granularity"
        }

        val sortBox = new ComboBox(Seq(
          "Timestamp Ascending",
          "Timestamp Descending",
          "Output Ascending",
          "Output Descending"
        )) {
          promptText = "Sort by"
        }

        val searchField = new TextField {
          promptText = "Search keyword (optional)"
        }

        val viewDataBtn = new Button("View Data") {
          onAction = _ => {
            val selected = datasetBox.value.value
            val granularity = granularityBox.value.value
            if (lastDateTag.nonEmpty && selected != null && granularity != null) {
              val path = dataDir.resolve(s"$selected-$lastDateTag.csv").toString
              val recs = CsvStorage.readRecords(path)
              if (recs.isEmpty) {
                outputArea.text = s"### $selected: no records"
              } else {
                val grouped = DataAnalyzer.groupByGranularity(recs, granularity)
                val sortedGrouped = sortBox.value.value match {
                  case "Timestamp Ascending"  => grouped.toSeq.sortBy(_._1)
                  case "Timestamp Descending" => grouped.toSeq.sortBy(_._1)(Ordering[String].reverse)
                  case "Output Ascending"     => grouped.toSeq.sortBy(_._2.map(_.outputKWh).sum)
                  case "Output Descending"    => grouped.toSeq.sortBy(_._2.map(_.outputKWh).sum)(Ordering[Double].reverse)
                  case _                      => grouped.toSeq
                }

                val searchKeyword = searchField.text.value.trim.toLowerCase
                val lines = sortedGrouped.map {
                  case (key, list) =>
                    val sum = list.map(_.outputKWh).sum
                    f"$key%-15s | ${list.head.source.id}%-12s | $sum%.2f kWh"
                }

                val filteredLines = if (searchKeyword.nonEmpty)
                  lines.filter(_.toLowerCase.contains(searchKeyword))
                else
                  lines

                outputArea.text = s"\n### $selected - $granularity\n" + filteredLines.mkString("\n")

                val chartFormatter = granularity match {
                  case "Hourly"   => DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00")
                  case "Daily"    => DateTimeFormatter.ofPattern("yyyy-MM-dd")
                  case "Weekly"   => DateTimeFormatter.ofPattern("'Week of' yyyy-MM-dd")
                  case "Monthly"  => DateTimeFormatter.ofPattern("yyyy-MM")
                  case _          => DateTimeFormatter.ISO_LOCAL_DATE_TIME
                }

                val chartData = sortedGrouped.zipWithIndex.map {
                  case ((_, list), idx) =>
                    val label = s"${idx + 1}"
                    val sum = list.map(_.outputKWh).sum
                    (label, sum)
                }.filter {
                  case (label, _) =>
                    searchField.text.value.trim.isEmpty || label.toLowerCase.contains(searchField.text.value.trim.toLowerCase)
                }

                chartSeries.data().clear()
                val displayData = if (granularity == "Hourly" || granularity == "Daily") {
                  chartData.sortBy(_._1).takeRight(50) // Show last 50 entries for clarity
                } else chartData

                chartSeries.data() ++= displayData.map { case (label, value) =>
                  XYChart.Data[String, Number](label, value)
                }
              }
            } else {
              outputArea.text = "Download data first and select dataset and granularity."
            }
          }
        }

        val analyzeBtn = new Button("Analyze") {
          onAction = _ => {
            val selected = analyzeBox.value.value
            val granularity = granularityBox.value.value
            if (lastDateTag.nonEmpty && selected != null && granularity != null) {
              val raw = if (selected == "All")
                Seq("solar", "wind", "hydro", "consumption").flatMap(label => CsvStorage.readRecords(dataDir.resolve(s"$label-$lastDateTag.csv").toString))
              else
                CsvStorage.readRecords(dataDir.resolve(s"$selected-$lastDateTag.csv").toString)

              val groupedTotals = DataAnalyzer.groupByGranularity(raw, granularity).mapValues(_.map(_.outputKWh).sum).values.toSeq
              if (groupedTotals.nonEmpty) {
                val mean = DataAnalyzer.mean(groupedTotals)
                val median = DataAnalyzer.median(groupedTotals)
                val mode = DataAnalyzer.mode(groupedTotals).mkString(", ")
                val range = DataAnalyzer.range(groupedTotals)
                val mid = DataAnalyzer.midrange(groupedTotals)
                outputArea.text = f"Dataset: $selected - $granularity\nMean: $mean%.2f\nMedian: $median%.2f\nMode: $mode\nRange: $range%.2f\nMidrange: $mid%.2f"
              } else {
                outputArea.text = s"No data available for $selected."
              }
            } else {
              outputArea.text = "Download data first and select dataset and granularity."
            }
          }
        }

        val lowThresholdField = new TextField {
          promptText = "Low output threshold"
        }

        val dropThresholdField = new TextField {
          promptText = "Drop % threshold"
        }

        val monitorBtn = new Button("Monitor") {
          onAction = _ => {
            if (lastDateTag.nonEmpty) {
              val all = Seq("solar", "wind", "hydro").flatMap { label =>
                CsvStorage.readRecords(dataDir.resolve(s"$label-$lastDateTag.csv").toString)
              }

              val lowThresh = Try(lowThresholdField.text.value.trim.toDouble).getOrElse(10.0)
              println(s"Low Threshold Used: $lowThresh")
              val dropThresh = Try(dropThresholdField.text.value.trim.toDouble).getOrElse(50.0)

              val lowAlerts = Monitor.detectLowOutput(all, lowThresh)
              val dropAlerts = Monitor.detectSuddenDrops(all, dropThresh)

              val output = new StringBuilder
              if (lowAlerts.nonEmpty) {
                output.append("⚠️ Low Output Alerts:\n")
                output.append(lowAlerts.map(a => f"${a.record.timestamp} | ${a.record.source.id} | ${a.record.outputKWh}%.2f kWh (threshold ${a.threshold})").mkString("\n"))
                output.append("\n\n")
              } else {
                output.append("No low output alerts.\n\n")
              }

              if (dropAlerts.nonEmpty) {
                output.append("⚠️ Sudden Drop Alerts:\n")
                output.append(dropAlerts.map(a => f"${a.current.timestamp} | ${a.current.source.id} | Drop: ${a.dropPercent}%%").mkString("\n"))
              }

              if (output.isEmpty)
                outputArea.text = "No alerts detected."
              else
                outputArea.text = output.toString()

            } else outputArea.text = "Download data first."
          }
        }

        root = new VBox(10) {
          children = Seq(
            new HBox(10, new Label("Single date:"), dateInput, downloadBtn),
            new HBox(10, new Label("Date range:"), rangeStart, rangeEnd, downloadRangeBtn),
            new HBox(10, new Label("Predefined range:"), predefinedBox, downloadPredefinedBtn),
            new HBox(10, datasetBox, viewDataBtn, searchField, analyzeBox, analyzeBtn, granularityBox, sortBox),
            new HBox(10, new Label("Low Threshold:"), lowThresholdField, new Label("Drop %:"), dropThresholdField, monitorBtn),
            lineChart,
            outputArea
          )
        }
      }
    }
  }
}