name := """DbMigration"""

organization := "jp.hotbrain"

version := "0.0.1-SNAPSHOT"

val ver_mysql = "5.1.48"

scalaVersion := "2.12.7"

crossScalaVersions := Seq("2.12.7", "2.13.1")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

crossPaths := true

libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
  "org.junit.jupiter" % "junit-jupiter-api" % "5.3.1" % Test
  // https://mvnrepository.com/artifact/org.junit.vintage/junit-vintage-engine
  , "org.junit.vintage" % "junit-vintage-engine" % "5.3.1" % Test
  // https://mvnrepository.com/artifact/org.junit.platform/junit-platform-launcher
  , "org.junit.platform" % "junit-platform-launcher" % "1.3.1" % Test

  // https://mvnrepository.com/artifact/org.scala-lang.modules/scala-parser-combinators
  , "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"

  , "mysql" % "mysql-connector-java" % ver_mysql
)

