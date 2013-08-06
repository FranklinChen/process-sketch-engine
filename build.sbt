import AssemblyKeys._

name := "process-sketch-engine"

organization := "com.franklinchen"

organizationHomepage := Some(url("http://franklinchen.com/"))

homepage := Some(url("http://github.com/FranklinChen/process-sketch-engine"))

startYear := Some(2013)

description := "A Scala project"

version := "1.0.0"

scalaVersion := "2.10.2"

scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies += "org.scala-lang" % "jline" % "2.10.2"

// command line and logging
libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "0.9.3",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1"
)

{
  val logbackVersion = "1.0.13"
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "ch.qos.logback" % "logback-core" % logbackVersion
  )
}

// HTTP Components 4.x
libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "fluent-hc" % "4.3-beta2"
)

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.5"

libraryDependencies += "com.github.theon" %% "scala-uri" % "0.3.5"

libraryDependencies += "org.mapdb" % "mapdb" % "0.9.3"

libraryDependencies ++= Seq(
  "com.google.gdata.gdata-java-client" % "gdata-spreadsheet-3.0" % "1.47.1",
  "com.google.oauth-client" % "google-oauth-client" % "1.16.0-rc"
)

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.10.1" % "test",
  "org.specs2" %% "specs2" % "2.1.1" % "test"
)

resolvers += Classpaths.typesafeReleases

resolvers += Classpaths.typesafeSnapshots

resolvers += "Burtsev.Net Maven Repository" at "http://maven.burtsev.net"

jarName in assembly := "NumberConverter.jar"

mainClass in assembly := Some("com.franklinchen.NumberConverter")

assemblySettings

mainClass := Some("com.franklinchen.NumberConverter")

proguardSettings

ProguardKeys.options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

ProguardKeys.options in Proguard += ProguardOptions.keepMain("com.franklinchen.NumberConverter")

// http://janalyse-series.googlecode.com/svn-history/r118/trunk/onejar/build.sbt
// jansi is embedded inside jline !
excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter {c=> List("jansi") exists {c.data.getName contains _} }
}
