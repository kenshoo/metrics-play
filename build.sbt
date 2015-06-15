organization:= "com.kenshoo"

name := "metrics-play"

version := "2.3.0_0.2.1"

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.10.4", scalaVersion.value)

testOptions in Test += Tests.Argument("junitxml", "console")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

val playVersion = "2.3.9"
val metricsVersion = "3.1.2"

libraryDependencies ++= Seq(
    "io.dropwizard.metrics" % "metrics-core" % metricsVersion,
    "io.dropwizard.metrics" % "metrics-json" % metricsVersion,
    "io.dropwizard.metrics" % "metrics-jvm" % metricsVersion,
    "io.dropwizard.metrics" % "metrics-logback" % metricsVersion,
    "com.typesafe.play" %% "play" % playVersion % "provided",
    //test
    "com.typesafe.play" %% "play-test" % playVersion % "test",
    "org.specs2" %% "specs2" % "2.3.12" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test"
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
