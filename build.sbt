name := """db-migration"""

organization := "jp.hotbrain"

version := "1.7.0-SNAPSHOT"

val ver_mysql = "8.0.33"

scalaVersion := "2.13.14"

crossScalaVersions := Seq("2.13.14")

javacOptions ++= Seq("-source", "21", "-target", "21", "-Xlint")

crossPaths := true

libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
  "org.junit.jupiter" % "junit-jupiter-api" % "5.13.4" % Test
  // https://mvnrepository.com/artifact/org.junit.vintage/junit-vintage-engine
  , "org.junit.vintage" % "junit-vintage-engine" % "5.13.4" % Test
  // https://mvnrepository.com/artifact/org.junit.platform/junit-platform-launcher
  , "org.junit.platform" % "junit-platform-launcher" % "1.13.4" % Test

  // https://mvnrepository.com/artifact/org.scala-lang.modules/scala-parser-combinators
  , "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0"

  , "mysql" % "mysql-connector-java" % ver_mysql
)
