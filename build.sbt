organization:= "com.kenshoo"

name := "metrics-play"

version := "2.6.2_0.6.1"

scalaVersion := "2.12.2"

crossScalaVersions := Seq("2.12.2")

scalacOptions := Seq("-unchecked", "-deprecation")

testOptions in Test += Tests.Argument("junitxml", "console")

parallelExecution in Test := false

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

resolvers += "specs2" at "https://mvnrepository.com/artifact/org.specs2/specs2_2.12"

libraryDependencies ++= Seq(
    "io.dropwizard.metrics" % "metrics-core" % "3.2.4",
    "io.dropwizard.metrics" % "metrics-json" % "3.2.4",
    "io.dropwizard.metrics" % "metrics-jvm" % "3.2.4",
    "io.dropwizard.metrics" % "metrics-logback" % "3.2.4",
    "com.typesafe.play" %% "play" % "2.6.2" % "provided",
    "org.joda" % "joda-convert" % "1.8.2",
    //test
    "com.typesafe.play" %% "play-test" % "2.6.2" % "test",
    "com.typesafe.play" %% "play-specs2" % "2.6.2" % "test",
    "org.specs2" %% "specs2" % "2.4.17" % "test"
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
