package DAO

import model.Candidate
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CandidateDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  // Table mapping
  class Candidates(tag: Tag) extends Table[Candidate](tag, "candidates") {
    def candidateId = column[String]("candidate_id", O.PrimaryKey)
    def candidateName = column[String]("candidate_name")
    def partyAffiliation = column[String]("party_affiliation")
    def biography = column[String]("biography")
    def campaignPlatform = column[String]("campaign_platform")
    def photoUrl = column[String]("photo_url")

    def * = (candidateId, candidateName, partyAffiliation, biography, campaignPlatform, photoUrl) <> (Candidate.tupled, Candidate.unapply)
  }

  val candidates = TableQuery[Candidates]

  def all(): Future[Seq[Candidate]] = db.run(candidates.result)

  def getCandidatesCount: Future[Int] = {
    val query = sql"SELECT count(*) AS candidates_count FROM candidates".as[Int]
    db.run(query).map(_.headOption.getOrElse(0))
  }

  def upsert(candidateList: Seq[Candidate]): Future[Seq[Int]] = {
    val toBeInserted = candidateList.map { candidates.insertOrUpdate }
    val inOneGo = DBIO.sequence(toBeInserted)
    db.run(inOneGo)
  }
}
