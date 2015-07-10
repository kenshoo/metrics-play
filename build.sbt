
organization := "com.kenshoo"

name := "metrics-play"

version := "2.4.0_0.1.9-r4_inbox"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.4", "2.11.2")

testOptions in Test += Tests.Argument("junitxml", "console")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "io.dropwizard.metrics" % "metrics-graphite" % "3.1.0",
  "io.dropwizard.metrics" % "metrics-core" % "3.1.0",
  "io.dropwizard.metrics" % "metrics-json" % "3.1.0",
  "io.dropwizard.metrics" % "metrics-jvm" % "3.1.0",
  "io.dropwizard.metrics" % "metrics-logback" % "3.1.0",
  "com.typesafe.play" %% "play" % "2.3.4" % "provided",
  //test
  "com.typesafe.play" %% "play-test" % "2.3.4" % "test",
  "org.specs2" %% "specs2" % "3.3.1" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test",
  "javax.inject" % "javax.inject" % "1"
)

libraryDependencies += specs2 % Test

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

publishMavenStyle := true

publishTo := Some("cody" at "http://cody:8082/nexus/content/repositories/releases")

credentials += Credentials(Path.userHome / ".sbt" / "sonatype.credentials")

pomIncludeRepository := { _ => false}

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
