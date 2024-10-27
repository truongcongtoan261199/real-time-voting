package services

import model.Candidate

import scala.concurrent.Future

trait CandidateServices {
  def listCandidates: Future[Seq[Candidate]]

  def getCandidatesCount: Future[Int]

  def upsertCandidates(candidateList: Seq[Candidate]): Unit

  def generateCandidateData(candidateNumber: Int, totalParties: Int): Future[Candidate]
}
