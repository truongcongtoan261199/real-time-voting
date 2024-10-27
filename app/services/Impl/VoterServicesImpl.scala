package services.Impl

import DAO.VoterDAO
import com.google.inject.Inject
import model.{Address, Voter}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.pekko.kafka.ProducerMessage
import org.apache.pekko.kafka.ProducerMessage.Message
import org.apache.pekko.kafka.scaladsl.Producer
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.apache.pekko.{Done, NotUsed}
import play.api.libs.json.{JsArray, JsNumber, JsString, Json}
import play.api.libs.ws.WSClient
import services.VoterServices
import uitilites.Const.BASE_URL
import uitilites.KafkaConfiguration.producerSettings

import scala.concurrent.{ExecutionContext, Future}

class VoterServicesImpl @Inject()(voterDAO: VoterDAO, ws: WSClient)(implicit ec: ExecutionContext, mat: Materializer) extends VoterServices {

  def listVoters: Future[Seq[Voter]] = voterDAO.all()

  def getVotersCount: Future[Int] = voterDAO.getVotersCount

  override def getVoter(id: String): Future[Option[Voter]] = voterDAO.findById(id)

  def generateVoterData(): Future[Voter] = {
    ws.url(BASE_URL).get().map { response =>
      if (response.status == 200) {
        val responseBody = response.json
        val result = (responseBody \ "results").as[JsArray].head
        val voterId = (result \ "login" \ "uuid").as[JsString].value
        val voterName = (result \ "name" \ "first").as[JsString].value + " " + (result \ "name" \ "last").as[JsString].value
        val dateOfBirth = (result \ "dob" \ "date").as[JsString].value
        val gender = (result \ "gender").as[JsString].value
        val nationality = (result \ "nat").as[JsString].value
        val registrationNumber = (result \ "login" \ "username").as[JsString].value
        val street = (result \ "location" \ "street" \ "number").as[JsNumber].value.toString() + " " + (result \ "location" \ "street" \ "name").as[JsString].value
        val city = (result \ "location" \ "city").as[JsString].value
        val state = (result \ "location" \ "state").as[JsString].value
        val country = (result \ "location" \ "country").as[JsString].value
        val postcode = (result \ "location" \ "postcode").as[JsString].value
        val email = (result \ "email").as[JsString].value
        val phoneNumber = (result \ "phone").as[JsString].value
        val cellNumber = (result \ "cell").as[JsString].value
        val picture = (result \ "picture" \ "large").as[JsString].value
        val registeredAge = (result \ "registered" \ "age").as[JsNumber].value.toInt

       Voter(
          voterId = voterId,
          voterName = voterName,
          dateOfBirth = dateOfBirth,
          gender = gender,
          nationality = nationality,
          registrationNumber = registrationNumber,
          address = Address(
            street = street,
            city = city,
            state = state,
            country = country,
            postcode = postcode
          ),
          email = email,
          phoneNumber = phoneNumber,
          cellNumber = cellNumber,
          picture = picture,
          registeredAge = registeredAge
        )
      } else throw new Exception("Failed to fetch data")
    }
  }

  override def upsertVoters(voters: Seq[Voter]): Unit = voterDAO.upsert(voters)

  def sendVoterMessagesStream(voters: Seq[Voter], topic: String): Future[Done] = {

    val kafkaProducerFlow = Flow[Voter]
      .map { voter =>
        val key = voter.voterId
        val value = Json.toJson(voter).toString()
        val record = new ProducerRecord[String, String](topic, key, value)
        Message(record, NotUsed)
      }
      .via(Producer.flexiFlow(producerSettings))
      .map {
        case ProducerMessage.Result(metadata, _) =>
          println(s"Produced message to Kafka: topic ${metadata.topic()}, partition ${metadata.partition()}, offset ${metadata.offset()}")
          metadata
      }

    Source(voters).via(kafkaProducerFlow).runWith(Sink.ignore)
  }
}
