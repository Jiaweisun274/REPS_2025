package reps

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import reps.models._
import java.time.LocalDateTime

class DataAnalyzerSpec extends AnyFlatSpec with Matchers {
  val recs = Seq(
    EnergyRecord(SolarPanel("A"), LocalDateTime.now, 10),
    EnergyRecord(SolarPanel("A"), LocalDateTime.now, 20),
    EnergyRecord(SolarPanel("A"), LocalDateTime.now, 20)
  )

  "mean" should "compute average" in {
    DataAnalyzer.mean(recs) shouldEqual 50.0/3
  }

  "median" should "pick middle" in {
    DataAnalyzer.median(recs) shouldEqual 20
  }

  "mode" should "return most frequent" in {
    DataAnalyzer.mode(recs) should contain only 20
  }
}