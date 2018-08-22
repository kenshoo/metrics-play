organization:= "com.kenshoo"

name := "metrics-play"

version := "2.5.9_0.5.1"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8")

testOptions in Test += Tests.Argument("junitxml", "console")

parallelExecution in Test := false

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

resolvers += "specs2" at "https://mvnrepository.com/artifact/org.specs2/specs2_2.11"

libraryDependencies ++= Seq(
    "io.dropwizard.metrics" % "metrics-core" % "3.2.2",
    "io.dropwizard.metrics" % "metrics-json" % "3.2.2",
    "io.dropwizard.metrics" % "metrics-jvm" % "3.2.2",
    "io.dropwizard.metrics" % "metrics-logback" % "3.2.2",
    "com.typesafe.play" %% "play" % "2.5.9" % "provided",
    "org.joda" % "joda-convert" % "1.2",
    //test
    "com.typesafe.play" %% "play-test" % "2.5.9" % "test",
    "com.typesafe.play" %% "play-specs2" % "2.5.9" % "test",
    "org.specs2" %% "specs2" % "2.4.15" % "test"
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
