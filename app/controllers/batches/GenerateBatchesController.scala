package controllers.batches

import batches.{GenerateCandidatesBatch, GenerateVotersBatch}
import com.google.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scala.concurrent.ExecutionContext

class GenerateBatchesController @Inject()(
  cc: ControllerComponents,
  generateCandidateBatch: GenerateCandidatesBatch,
  generateVotersBatch: GenerateVotersBatch
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  def generateCandidates(): Action[AnyContent] = Action {
    generateCandidateBatch.generateCandidates()
    Ok("Candidates generating Done !!!")
  }

  def generateVoters(): Action[AnyContent] = Action {
    generateVotersBatch.generateVoters()
    Ok("Voters generating Done !!!")
  }
}
