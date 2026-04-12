name := """db-migration"""

organization := "jp.hotbrain"

version := "1.7.0-SNAPSHOT"

scalaVersion := "2.13.18"

crossScalaVersions := Seq(scalaVersion.value)

javacOptions ++= Seq("-source", "21", "-target", "21", "-Xlint")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

crossPaths := true

libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
  "org.junit.jupiter" % "junit-jupiter-api" % "6.0.3" % Test,
  // https://mvnrepository.com/artifact/org.junit.vintage/junit-vintage-engine
  "org.junit.vintage" % "junit-vintage-engine" % "6.0.3" % Test,
  // https://mvnrepository.com/artifact/org.junit.platform/junit-platform-launcher
  "org.junit.platform" % "junit-platform-launcher" % "6.0.3" % Test,

  // https://mvnrepository.com/artifact/org.scala-lang.modules/scala-parser-combinators
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",

  "mysql" % "mysql-connector-java" % "8.0.33" % Test,
)
