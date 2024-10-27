package batches

import com.google.inject.Inject
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import services.CandidateServices
import uitilites.Common.retry

import scala.concurrent.ExecutionContext

class GenerateCandidatesBatch @Inject()(
  candidateService: CandidateServices
)(implicit mat: Materializer, ec: ExecutionContext) {
  def generateCandidates(): Unit = {

    val generateFlow = Flow[Int]
      .mapAsync(4) { i =>
        retry(3)(candidateService.generateCandidateData(i, 3))
      }
      .grouped(500)
      .map(candidateService.upsertCandidates)

    Source(1 to 100).via(generateFlow).runWith(Sink.ignore).onComplete {
      case scala.util.Success(_) => println("Done generate Voters")
      case scala.util.Failure(exception) => println(s"Stream processing failed: ${exception.getMessage}")
    }
  }
}
