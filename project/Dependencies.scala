import sbt._

object Dependencies {
  lazy val SPARK_VERSION = "3.5.1"
  lazy val SLICK_VERSION = "5.3.0"
  lazy val POSTGRESQL_VERSION = "42.7.3"
  lazy val CONFLUENT_KAFKA_VERSION = "7.3.4"
  lazy val KAFKA_VERSION = "3.7.0"
  lazy val PLAY_JSON_VERSION = "3.0.4"
  lazy val SLICK_EVOLUTION_VERSION = "5.3.0"
  lazy val WEB_SOCKET_VERSION = "3.0.4"
  lazy val PARSER_COMBINATOR_VERSION = "2.4.0"

  lazy val scalaTest = "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1"
  lazy val sparkCore = "org.apache.spark" %% "spark-core" % SPARK_VERSION
  lazy val sparkStreaming = "org.apache.spark" %% "spark-streaming" % SPARK_VERSION
  lazy val sparkSQL = "org.apache.spark" %% "spark-sql" % SPARK_VERSION
  lazy val slick = "com.typesafe.play" %% "play-slick" % SLICK_VERSION
  lazy val slickEvolution = "com.typesafe.play" %% "play-slick-evolutions" % SLICK_EVOLUTION_VERSION
  lazy val postgresql = "org.postgresql" % "postgresql" % POSTGRESQL_VERSION
  lazy val confluentKafka = "io.confluent" % "kafka-avro-serializer" % CONFLUENT_KAFKA_VERSION
  lazy val kafka = "org.apache.kafka" % "kafka-clients" % KAFKA_VERSION
  lazy val playJson = "org.playframework" %% "play-json" % PLAY_JSON_VERSION
  lazy val webSocket = "org.playframework" %% "play-ahc-ws" % WEB_SOCKET_VERSION

  lazy val pekkoConnectorsKafka = "org.apache.pekko" %% "pekko-connectors-kafka" % "1.0.0"

  lazy val parserCombinator = "org.scala-lang.modules" %% "scala-parser-combinators" % PARSER_COMBINATOR_VERSION
  lazy val sparkSqlKafka = "org.apache.spark" %% "spark-sql-kafka-0-10" % SPARK_VERSION
}
