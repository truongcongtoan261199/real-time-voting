package batches

import com.google.inject.Inject
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import services.VoterServices
import uitilites.Common.retry
import uitilites.Const.RETRY_TIMES

import scala.concurrent.ExecutionContext

class GenerateVotersBatch @Inject()(
  voterServices: VoterServices
)(implicit system: ActorSystem, ec: ExecutionContext) {

  def generateVoters(): Unit = {
    println("====== Start generating voters batch ======")
    val flow = Flow[Int]
      .mapAsync(4) { _ =>
        retry(RETRY_TIMES)(voterServices.generateVoterData())
      }
      .grouped(500)
      .map(voterServices.upsertVoters)
    Source(1 to 1000).via(flow).runWith(Sink.ignore).onComplete {
      case scala.util.Success(_) => println("Done generate Voters")
      case scala.util.Failure(exception) => println(s"Stream processing failed: ${exception.getMessage}")
    }
  }
}
