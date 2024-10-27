import Dependencies._

name := """realtimeVoting"""
organization := "com.example"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.14"

libraryDependencies ++= Seq(
  guice,
  webSocket,
  scalaTest % Test,
  sparkCore,
  sparkStreaming,
  sparkSQL,
  slick,
  slickEvolution,
  postgresql,
  kafka,
  playJson,
  sparkSqlKafka,
  pekkoConnectorsKafka,
  "org.webjars.npm" % "chartist" % "1.3.0"
)
dependencyOverrides ++= Seq(
  parserCombinator
)


