package model

import play.api.libs.json.{Json, OFormat}

case class Candidate(
  candidateId: String,
  candidateName: String,
  partyAffiliation: String,
  biography: String,
  campaignPlatform: String,
  photoUrl: String
)

object Candidate {
  implicit val format: OFormat[Candidate] = Json.format[Candidate]
}
