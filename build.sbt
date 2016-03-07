organization:= "com.kenshoo"

name := "metrics-play"

version := "2.4.6.0-SNAPSHOT"

scalaVersion := "2.11.7"

testOptions in Test += Tests.Argument("junitxml", "console")

val strikeadRepo = "http://nexus.strikead.com:8081/nexus/content/repositories/"
val strikeadSnapshots = "StrikeAd Snapshots" at strikeadRepo + "snapshots"
val strikeadReleases = "StrikeAd Releases" at strikeadRepo + "releases"

resolvers ++= Seq(
  strikeadSnapshots,
  strikeadReleases
)

libraryDependencies ++= Seq(
    "io.dropwizard.metrics" % "metrics-core" % "3.1.2",
    "io.dropwizard.metrics" % "metrics-json" % "3.1.2",
    "io.dropwizard.metrics" % "metrics-jvm" % "3.1.2",
    "io.dropwizard.metrics" % "metrics-logback" % "3.1.2",
    "com.typesafe.play" %% "play" % "2.4.6" % "provided",
    "com.typesafe.play" %% "play-test" % "2.4.6" % "test",
    "com.typesafe.play" %% "play-specs2" % "2.4.6" % "test",
    "org.specs2" %% "specs2" % "2.3.12" % "test"
)

publishMavenStyle := true

publishTo := Some(if (isSnapshot.value) strikeadSnapshots else strikeadReleases)

pomIncludeRepository := { _ => false }

publishArtifact in Test := false

pomExtra := (
  <url>https://github.com/kenshoo/metrics-play</url>
  <inceptionYear>2013</inceptionYear>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
      <comments>A business-friendly OSS license</comments>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:kenshoo/metrics-play.git</url>
    <connection>scm:git@github.com:kenshoo/metrics-play.git</connection>
  </scm>
  <developers>
    <developer>
      <name>Lior Harel</name>
      <email>harel.lior@gmail.com</email>
      <roles>
        <role>Author</role>
      </roles>
      <organization>Kenshoo</organization>
    </developer>
  </developers>)
