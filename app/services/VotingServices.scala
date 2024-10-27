package services

import DAO.VoteDAO
import com.google.inject.Inject
import model.{Vote, VoteWithVoterAndCandidate, Voter}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.pekko.kafka.scaladsl.{Consumer, Producer}
import org.apache.pekko.kafka.{ProducerMessage, Subscriptions}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import uitilites.Const.{VOTER_TOPIC, VOTES_TOPIC}
import uitilites.KafkaConfiguration.{consumerSettings, producerSettings}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Random, Success}

class VotingServices @Inject()(voteDAO: VoteDAO, candidateServices: CandidateServices)(implicit executionContext: ExecutionContext, mat: Materializer) {

  def voting(): Unit = {
    val candidateFuture = candidateServices.listCandidates
    candidateFuture.onComplete {
      case Success(candidates) =>
        if (candidates.nonEmpty) {

          // Kafka Consumer Source
          val kafkaConsumerSource: Source[ConsumerRecord[String, String], Consumer.Control] =
            Consumer.plainSource(consumerSettings, Subscriptions.topics(VOTER_TOPIC)).takeWithin(15.seconds)

          // Flow to process each Kafka message and upsert to DB using .map
          val processFlow = Flow[ConsumerRecord[String, String]]
            .map { record =>
              val msgValue = record.value()
              if (msgValue.nonEmpty) {
                val voter = Json.parse(msgValue).as[Voter]
                val chosenCandidate = candidates(Random.nextInt(candidates.size))
                val nowUtc = DateTime.now(org.joda.time.DateTimeZone.UTC)
                val formattedDate = nowUtc.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
                val voteWithCandidate = VoteWithVoterAndCandidate(
                  voter,
                  chosenCandidate,
                  votingTime = formattedDate,
                  vote = Option(1)
                )
                val vote = Vote(
                  voterId = voter.voterId,
                  chosenCandidate.candidateId,
                  votingTime = formattedDate,
                  vote = Option(1)
                )
                sendVotesMessages(voteWithCandidate, VOTES_TOPIC)
                vote
              } else {
                println("Empty message value")
                throw new RuntimeException("Empty message value")
              }
            }
            .grouped(300)
            .map {voteDAO.upsert}

          kafkaConsumerSource.via(processFlow).runWith(Sink.ignore)
        } else {
          throw new Exception("No candidates found in the database")
        }

      case Failure(exception) => throw new Exception(s"Cannot get candidates from the database: ${exception.getMessage}")
    }
  }

  private def sendVotesMessages(voteWithCandidate: VoteWithVoterAndCandidate, topic: String) = {
    val key = voteWithCandidate.voter.voterId
    val value = Json.toJson(voteWithCandidate).toString()
    val record = new ProducerRecord[String, String](topic, key, value)

    val message = ProducerMessage.single(record)

    Source.single(message)
      .via(Producer.flexiFlow(producerSettings))
      .map(_.passThrough)
      .runWith(Sink.head)
  }
}
