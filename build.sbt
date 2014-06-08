organization:= "com.kenshoo"

name := "metrics-play"

version := "2.3.0_0.1.6"

scalaVersion := "2.10.2"

crossScalaVersions := Seq("2.10.2", "2.11.1")

testOptions in Test += Tests.Argument("junitxml", "console")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "com.codahale.metrics" % "metrics-core" % "3.0.1",
    "com.codahale.metrics" % "metrics-json" % "3.0.1",
    "com.codahale.metrics" % "metrics-jvm" % "3.0.1",
    "com.typesafe.play" %% "play" % "2.3.0" % "provided",
    //test
    "com.typesafe.play" %% "play-test" % "2.3.0" % "test",
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
