package controllers

import com.google.inject.Inject
import model.{Address, Voter}
import org.apache.kafka.clients.producer._
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import services.{CandidateServices, SparkStreamingService, VoterServices, VotingServices}
import uitilites.Const.{AGGREGATED_TURNOUT_BY_LOCATION, AGGREGATED_VOTES_PER_CANDIDATE, VOTER_TOPIC}
import uitilites.KafkaConfiguration.fetchDataFromKafka

import java.text.SimpleDateFormat
import java.util.{Date, Properties}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class MainController @Inject()(
  cc: ControllerComponents,
  voterServices: VoterServices,
  votingServices: VotingServices,
  candidateServices: CandidateServices,
  sparkStreamingService: SparkStreamingService
)(implicit system: ActorSystem, ec: ExecutionContext) extends AbstractController(cc) {
  implicit val addressFormat: OFormat[Address] = Json.format[Address]
  implicit val voterFormat: OFormat[Voter] = Json.format[Voter]

  private val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")

  //create kafka producer
  val producer = new KafkaProducer[String, String](props)

  def sendVoterMessagesStream: Action[AnyContent] = Action.async {
    voterServices.listVoters.onComplete {
      case Success(voters) =>
        if (voters.isEmpty) {
          println("No voters from DB !!!")
        } else {
          voterServices.sendVoterMessagesStream(voters, VOTER_TOPIC)
        }
      case Failure(e) => println(e.getMessage)
    }
    Future(Ok("ok"))
  }

  def stream: Action[AnyContent] = Action {
    sparkStreamingService.stream()
    Ok("ok")
  }

  def voting: Action[AnyContent] = Action {
    votingServices.voting()
    Ok("Ok")
  }

  def index: Action[AnyContent] = Action.async { _ =>

    // get raw data
    val candidatesFuture: Future[Seq[(String, Int, String, String)]] = fetchDataFromKafka(AGGREGATED_VOTES_PER_CANDIDATE).map { data =>
      data.map { json =>
        val candidateName = (json \ "candidateName").as[String]
        val voteCount = (json \ "totalVotes").as[Int]
        val photoUrl = (json \ "photoUrl").asOpt[String].getOrElse("/assets/images/default_candidate.png")
        val partyAffiliation = (json \ "partyAffiliation").asOpt[String].getOrElse("Unknown")
        (candidateName, voteCount, photoUrl, partyAffiliation)
      }
    }

    val turnOutByLocationsFuture = fetchDataFromKafka(AGGREGATED_TURNOUT_BY_LOCATION).map { data =>
      data.map { json =>
        val locationName = (json \ "state").as[String]
        val voteCount = (json \ "count").as[Int]
        (locationName, voteCount)
      }
    }

    // count voters and candidates
    val votersCountFuture = voterServices.getVotersCount
    val candidatesCountFuture = candidateServices.getCandidatesCount


    val combinedFuture = for {
      votersCount <- votersCountFuture
      candidatesCount <- candidatesCountFuture
      candidates <- candidatesFuture
      turnOutByLocations <- turnOutByLocationsFuture
    } yield {
      val lastUpdateTime = new Date()
      val formattedLastUpdateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastUpdateTime)
      val topCandidates = candidates.sortBy(-_._2).take(3)
      val votes = candidates.map(_._2).sum
      val topPercentCandidates = candidates.sortBy(-_._2).take(3).map { case (candidateName, candidateVotes, _, _) =>
        val percentage = (candidateVotes.toDouble / votes) * 100
        (candidateName, percentage)
      }
      val leadingCandidate = candidates.maxBy(_._2)

      Ok(views.html.dashboard(votersCount, candidatesCount, topCandidates, topPercentCandidates, leadingCandidate, turnOutByLocations, formattedLastUpdateTime))
    }

    combinedFuture.recover {
      case ex: Exception =>
        InternalServerError(s"Error fetching data from Kafka or database: ${ex.getMessage}")
    }
  }


}

