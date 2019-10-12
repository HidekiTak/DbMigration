name := """DbMigrationSample"""

organization := "jp.hotbrain"

version := "0.0.1-SNAPSHOT"

val ver_mysql = "5.1.48"

scalaVersion := "2.12.7"

crossScalaVersions := Seq("2.12.7", "2.13.1")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

crossPaths := true

libraryDependencies ++= Seq(
  "jp.hotbrain" %% "dbmigration" % "0.0.1-SNAPSHOT"

  // https://mvnrepository.com/artifact/org.scala-lang.modules/scala-parser-combinators
  , "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"

  , "mysql" % "mysql-connector-java" % ver_mysql
)
