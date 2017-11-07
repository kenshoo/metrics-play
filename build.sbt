organization:= "com.kenshoo"

name := "metrics-play"
version := "2.5.0_0.5.0"
scalaVersion := "2.11.8"

testOptions in Test += Tests.Argument("junitxml", "console")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
    "io.dropwizard.metrics" % "metrics-core" % "3.1.2",
    "io.dropwizard.metrics" % "metrics-json" % "3.1.2",
    "io.dropwizard.metrics" % "metrics-jvm" % "3.1.2",
    "io.dropwizard.metrics" % "metrics-logback" % "3.1.2",
    "com.typesafe.play" %% "play" % "2.5.18" % "provided",
    "org.joda" % "joda-convert" % "1.2",
    //test
    "com.typesafe.play" %% "play-test" % "2.5.18" % "test",
    "com.typesafe.play" %% "play-specs2" % "2.5.18" % "test",
    "org.specs2" %% "specs2" % "2.3.12" % "test"
)

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

publishArtifact in Test := false
