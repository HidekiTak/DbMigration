name := """db-migration"""

organization := "jp.hotbrain"

version := "1.7.0-SNAPSHOT"

val ver_mysql = "8.0.33"

scalaVersion := "2.13.16"

crossScalaVersions := Seq(scalaVersion.value)

javacOptions ++= Seq("-source", "21", "-target", "21", "-Xlint")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

crossPaths := true

libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
  "org.junit.jupiter" % "junit-jupiter-api" % "5.13.4" % Test
  // https://mvnrepository.com/artifact/org.junit.vintage/junit-vintage-engine
  , "org.junit.vintage" % "junit-vintage-engine" % "5.13.4" % Test
  // https://mvnrepository.com/artifact/org.junit.platform/junit-platform-launcher
  , "org.junit.platform" % "junit-platform-launcher" % "1.13.4" % Test

  // https://mvnrepository.com/artifact/org.scala-lang.modules/scala-parser-combinators
  , "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"

  , "mysql" % "mysql-connector-java" % ver_mysql
)
