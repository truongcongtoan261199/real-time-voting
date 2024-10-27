package uitilites

object Const {
  lazy val BASE_URL = "https://randomuser.me/api/?nat=gb"
  lazy val PARTIES: List[String] = List("Management Party", "Savior Party", "Tech Republic Party")
  lazy val CANDIDATE_AMOUNT = 100
  lazy val PARTIES_AMOUNT = 3
  lazy val VOTER_TOPIC = "voters_topic"
  lazy val VOTES_TOPIC = "votes_topic"
  lazy val AGGREGATED_TURNOUT_BY_LOCATION = "aggregated_turnout_by_location"
  lazy val AGGREGATED_VOTES_PER_CANDIDATE = "aggregated_votes_per_candidate"
  lazy val INSERT_BATCH_SIZE= 10000
  lazy val RETRY_TIMES = 3
}
