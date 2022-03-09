organization:= "com.kenshoo"

name := "metrics-play"

scalaVersion := "2.13.8"

crossScalaVersions := Seq(scalaVersion.value, "2.12.8")

val playVersion = "2.8.11"

val metricsPlayVersion = "0.9.0-SNAPSHOT"

val dropwizardVersion = "4.0.5"

version := s"${playVersion}_${metricsPlayVersion}"


scalacOptions := Seq("-unchecked", "-deprecation")

testOptions in Test += Tests.Argument("junitxml", "console")

parallelExecution in Test := false

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "io.dropwizard.metrics" % "metrics-core" % dropwizardVersion,
    "io.dropwizard.metrics" % "metrics-json" % dropwizardVersion,
    "io.dropwizard.metrics" % "metrics-jvm" % dropwizardVersion,
    "io.dropwizard.metrics" % "metrics-logback" % dropwizardVersion,
    "com.typesafe.play" %% "play" % playVersion % Provided,
    "org.joda" % "joda-convert" % "2.2.0",
    "io.micrometer" % "micrometer-core" % "1.8.3",

    //Test
    "com.typesafe.play" %% "play-test" % playVersion % Test,
    "com.typesafe.play" %% "play-specs2" % playVersion % Test,
    "io.micrometer" % "micrometer-registry-jmx" % "1.8.3" % Test
)

publishMavenStyle := true

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

credentials += Credentials(Path.userHome / ".m2" / ".credentials")

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
        <name>Ran Nisim</name>
        <email>rannisim@gmail.com</email>
        <roles>
          <role>Author</role>
        </roles>
        <organization>Kenshoo</organization>
      </developer>
      <developer>
        <name>Lior Harel</name>
        <email>harel.lior@gmail.com</email>
        <roles>
          <role>Author</role>
        </roles>
        <organization>Kenshoo</organization>
      </developer>
    </developers>
  )
