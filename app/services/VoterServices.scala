package services

import model.Voter
import org.apache.pekko.Done

import scala.concurrent.Future

trait VoterServices {
  def listVoters: Future[Seq[Voter]]

  def getVotersCount: Future[Int]

  def getVoter(id: String): Future[Option[Voter]]

  def upsertVoters(voters: Seq[Voter]): Unit

  def generateVoterData(): Future[Voter]

  def sendVoterMessagesStream(voters: Seq[Voter], topic: String): Future[Done]
}
