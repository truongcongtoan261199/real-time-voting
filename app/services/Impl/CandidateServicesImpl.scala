package services.Impl

import DAO.CandidateDAO
import com.google.inject.Inject
import model.Candidate
import play.api.libs.json.{JsArray, JsString}
import play.api.libs.ws.WSClient
import services.CandidateServices
import uitilites.Const.{BASE_URL, PARTIES}

import scala.concurrent.{ExecutionContext, Future}

class CandidateServicesImpl @Inject()(
  candidateDAO: CandidateDAO,
  ws: WSClient)(implicit ec: ExecutionContext) extends CandidateServices {

  def listCandidates: Future[Seq[Candidate]] = {
    candidateDAO.all().map(el => el)
  }

  def getCandidatesCount: Future[Int] = candidateDAO.getCandidatesCount

  def generateCandidateData(candidateNumber: Int, totalParties: Int): Future[Candidate] = {
    val builtUrl = s"$BASE_URL&gender=${if (candidateNumber % 2 == 1) "female" else "male"}"
    ws.url(builtUrl).get().map { response =>
      if (response.status == 200) {
        val responseBody = response.json
        val result = (responseBody \ "results").as[JsArray].head
        val candidateId = (result \ "login" \ "uuid").as[JsString].value
        val candidateName = (result \ "name" \ "first").as[JsString].value + " " + (result \ "name" \ "last").as[JsString].value
        val photo = (result \ "picture" \ "large").as[JsString].value
        Candidate(
          candidateId = candidateId,
          candidateName = candidateName,
          partyAffiliation = PARTIES(candidateNumber % totalParties),
          biography = "A brief bio of the candidate.",
          campaignPlatform = "Key campaign promises or platform.",
          photoUrl = photo
        )
      } else throw new Exception("Failed to fetch data")
    }
  }

  override def upsertCandidates(candidateList: Seq[Candidate]): Unit = candidateDAO.upsert(candidateList)
}
