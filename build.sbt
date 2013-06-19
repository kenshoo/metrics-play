organization:= "com.kenshoo"

name := "play-metrics"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.0"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

testOptions in Test += Tests.Argument("junitxml", "console")

libraryDependencies ++= Seq(
    "com.codahale.metrics" % "metrics-core" % "3.0.0",
    "com.codahale.metrics" % "metrics-json" % "3.0.0",
    "com.codahale.metrics" % "metrics-jvm" % "3.0.0",
    "play" %% "play" % "2.1.0" % "provided",
    //test
    "play" %% "play-test" % "2.1.0" % "test",
    "org.specs2" % "specs2_2.10" % "1.13" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test"
)
