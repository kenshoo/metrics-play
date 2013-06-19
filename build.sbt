name := "play-metrics"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
    "com.codahale.metrics" % "metrics-core" % "3.0.0",
    "com.codahale.metrics" % "metrics-json" % "3.0.0",
    "com.codahale.metrics" % "metrics-jvm" % "3.0.0",
//    "play" % "play" % "2.1.0" % "provided",
    "org.specs2" % "specs2_2.10" % "1.13" % "test"
)
