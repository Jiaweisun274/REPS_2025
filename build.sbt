ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "REPS_2025",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core"    % "3.9.0",
      "com.softwaremill.sttp.client3" %% "circe"   % "3.9.0",
      "io.circe"                      %% "circe-generic" % "0.14.5",
      "org.scalafx"                  %% "scalafx" % "21.0.0-R32",
      "com.github.tototoshi"         %% "scala-csv" % "1.3.10",
      "org.scalatest"                %% "scalatest" % "3.2.16" % Test
    ),
    testFrameworks += new TestFramework("org.scalatest.tools.Framework")
  )