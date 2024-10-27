package DAO

import model.Vote
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import uitilites.Const.INSERT_BATCH_SIZE

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class VoteDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  // Table mapping
  class Votes(tag: Tag) extends Table[Vote](tag, "votes") {
    def voter_id = column[String]("voter_id", O.PrimaryKey)

    def candidate_id = column[String]("candidate_id", O.PrimaryKey)

    def voting_time = column[String]("voting_time")

    def vote = column[Option[Int]]("vote", O.Default(Some(1)))

    def * = (voter_id, candidate_id, voting_time, vote) <> (Vote.tupled, Vote.unapply)
  }

  private val votes = TableQuery[Votes]

  val truncateAction = sqlu"TRUNCATE TABLE votes";

  def truncateTable: Int = {
    val queryFuture = db.run(truncateAction)
    Await.result(queryFuture, 10.seconds)
  }

  def all(): Future[Seq[Vote]] = db.run(votes.result)

  def insert(voteList: List[Vote]) = {
    val insertFuture = db.run(votes ++= voteList)
    insertFuture.onComplete(_ => {
      println("Insert done  !!!")
    })
  }

  def upsert(voteList: Seq[Vote]): Future[Seq[Int]] = {
      val insertOrUpdateAction = voteList.map { vote =>
        sqlu"""
      INSERT INTO votes (voter_id, candidate_id, voting_time, vote)
      VALUES (${vote.voterId}, ${vote.candidateId}, ${vote.votingTime}, ${vote.vote.getOrElse(0)})
      ON CONFLICT (voter_id)
      DO UPDATE SET candidate_id = EXCLUDED.candidate_id,
                    voting_time = EXCLUDED.voting_time,
                    vote = EXCLUDED.vote
    """
      }

    db.run(DBIO.sequence(insertOrUpdateAction).transactionally).recoverWith(e => {
      println(e.getMessage)
      Future.successful(Seq.empty)
    })
  }

  def delete(voterId: String, candidateId: String): Future[Int] = db.run(votes.filter(vote => vote.voter_id === voterId && vote.candidate_id === candidateId).delete)
}
