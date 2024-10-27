package DAO

import model.{Address, Voter}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class VoterDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  // Table mapping
  class voters(tag: Tag) extends Table[Voter](tag, "voters") {
    def voterId = column[String]("voter_id", O.PrimaryKey)
    def voterName = column[String]("voter_name")
    def dateOfBirth = column[String]("date_of_birth")
    def gender = column[String]("gender")
    def nationality = column[String]("nationality")
    def registrationNumber = column[String]("registration_number")
    def addressStreet = column[String]("address_street")
    def addressCity = column[String]("address_city")
    def addressState = column[String]("address_state")
    def addressCountry = column[String]("address_country")
    def addressPostcode = column[String]("address_postcode")
    def email = column[String]("email")
    def phoneNumber = column[String]("phone_number")
    def cellNumber = column[String]("cell_number")
    def picture = column[String]("picture")
    def registeredAge = column[Int]("registered_age")
    def address = (addressStreet, addressCity, addressState, addressCountry, addressPostcode) <> (Address.tupled, Address.unapply)

    def * = (
      voterId,
      voterName,
      dateOfBirth,
      gender,
      nationality,
      registrationNumber,
      address,
      email,
      phoneNumber,
      cellNumber,
      picture,
      registeredAge) <> (Voter.tupled, Voter.unapply)
  }

  val voters = TableQuery[voters]

  def all(): Future[Seq[Voter]] = db.run(voters.result)

  def findById(id: String): Future[Option[Voter]] = db.run(voters.filter(_.voterId === id).result.headOption)

  def upsert(voterList: Seq[Voter]): Future[Seq[Int]] = {
    val toBeInserted = voterList.map {
      voters.insertOrUpdate
    }
    val inOneGo = DBIO.sequence(toBeInserted)
    db.run(inOneGo)
  }

  def getVotersCount: Future[Int] = {
    val query = sql"SELECT count(*) AS voters_count FROM voters".as[Int]
    db.run(query).map(_.headOption.getOrElse(0))
  }

  def delete(id: String): Future[Int] = db.run(voters.filter(_.voterId === id).delete)
}
