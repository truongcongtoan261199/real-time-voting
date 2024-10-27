package services

import org.apache.spark.sql.functions.{col, from_json}
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SparkSession, functions}
import uitilites.Const.{AGGREGATED_TURNOUT_BY_LOCATION, AGGREGATED_VOTES_PER_CANDIDATE, VOTES_TOPIC}

class SparkStreamingService {
  def stream(): Unit = {
    val spark = SparkSession.builder()
      .master("local[*]")
      .appName("ElectionAnalysis")
      .config("spark.sql.adaptive.enabled", "false")
      .getOrCreate()

    val candidateSchema = StructType(Seq(
      StructField("candidateId", StringType, nullable = true),
      StructField("candidateName", StringType, nullable = true),
      StructField("partyAffiliation", StringType, nullable = true),
      StructField("biography", StringType, nullable = true),
      StructField("campaignPlatform", StringType, nullable = true),
      StructField("photoUrl", StringType, nullable = true)
    ))

    val addressSchema = StructType(Seq(
      StructField("street", StringType, nullable = true),
      StructField("city", StringType, nullable = true),
      StructField("state", StringType, nullable = true),
      StructField("country", StringType, nullable = true),
      StructField("postcode", StringType, nullable = true),
    ))

    val voterSchema = StructType(Seq(
      StructField("voterId", StringType, nullable = true),
      StructField("voterName", StringType, nullable = true),
      StructField("dateOfBirth", StringType, nullable = true),
      StructField("gender", StringType, nullable = true),
      StructField("nationality", StringType, nullable = true),
      StructField("registrationNumber", StringType, nullable = true),
      StructField("address", addressSchema, nullable = true),
      StructField("email", StringType, nullable = true),
      StructField("phoneNumber", StringType, nullable = true),
      StructField("cellNumber", StringType, nullable = true),
      StructField("picture", StringType, nullable = true),
      StructField("registeredAge", IntegerType, nullable = true)
    ))

    val voteSchema = StructType(Seq(
      StructField("voter", voterSchema, nullable = true),
      StructField("candidate", candidateSchema, nullable = true),
      StructField("votingTime", StringType, nullable = false),
      StructField("vote", IntegerType, nullable = true)
    ))

    val kafkaStream = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", VOTES_TOPIC)
      .option("startingOffsets", "earliest")
      .load()

    val parsedStream = kafkaStream
      .selectExpr("CAST(value AS STRING)")
      .select(from_json(col("value").cast("string"), voteSchema).as("data"))
      .select("data.*")

    val processedStream = parsedStream
      .withColumn("voterId", col("voter.voterId"))
      .withColumn("candidateId", col("candidate.candidateId"))
      .withColumn("candidateName", col("candidate.candidateName"))
      .withColumn("partyAffiliation", col("candidate.partyAffiliation"))
      .withColumn("photoUrl", col("candidate.photoUrl"))
      .withColumn("state", col("voter.address.state"))
      .withColumn("votingTime", col("votingTime").cast(TimestampType))
      .withColumn("vote", col("vote").cast(IntegerType))

    val enrichedVotesDf = processedStream.withWatermark("votingTime", "1 minute")

    //     Aggregate votes per candidate and turnout by location
    val votesPerCandidate = enrichedVotesDf.groupBy(
      "candidateId",
      "candidateName",
      "partyAffiliation",
      "photoUrl")
      .agg(functions.sum("vote").alias("totalVotes"));

    val turnoutByLocation = enrichedVotesDf.groupBy("voter.address.state").count().alias("total_votes");

    val votesPerCandidateToKafka = votesPerCandidate
      .selectExpr("to_json(struct(*)) AS value")
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("topic", AGGREGATED_VOTES_PER_CANDIDATE)
      .option("checkpointLocation", "/Users/toan_tc/Documents/Spark/projects/realtimevoting/checkPoints/checkpoint1")
      .outputMode("update")
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()

    val turnoutByLocationToKafka = turnoutByLocation
      .selectExpr("to_json(struct(*)) AS value")
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("topic", AGGREGATED_TURNOUT_BY_LOCATION)
      .option("checkpointLocation", "/Users/toan_tc/Documents/Spark/projects/realtimevoting/checkPoints/checkpoint2")
      .outputMode("update")
      .start()

    votesPerCandidateToKafka.awaitTermination()
    turnoutByLocationToKafka.awaitTermination()

  }
}
