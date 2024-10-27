package model

import org.joda.time.DateTime
import play.api.libs.json.{JsString, Json, Writes}

// Case class cho Vote
case class Vote(
  voterId: String,
  candidateId: String,
  votingTime: String,
  vote: Option[Int] = Some(1)
)

object Vote {
  implicit val voteWrites: Writes[Vote] = Json.writes[Vote]
}

// Case class cho VoteWithVoterAndCandidate
case class VoteWithVoterAndCandidate(
  voter: Voter,
  candidate: Candidate,
  votingTime: String,
  vote: Option[Int] = Some(1)
)

object VoteWithVoterAndCandidate {
  implicit val jodaDateTimeWrites: Writes[DateTime] = (dateTime: DateTime) => JsString(dateTime.toString("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))

  implicit val voteWrites: Writes[VoteWithVoterAndCandidate] = Json.writes[VoteWithVoterAndCandidate]
}
