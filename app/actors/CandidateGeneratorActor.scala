package actors

import services.CandidateServices
import org.apache.pekko.actor.Actor
import uitilites.Const.{CANDIDATE_AMOUNT, PARTIES_AMOUNT}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.DurationInt

object CandidateGeneratorActor {
  case object GenerateCandidates
  case object StopGenerating
}

class CandidateGeneratorActor (candidateService: CandidateServices, promise: Promise[Unit]) extends Actor {

  import CandidateGeneratorActor._
  import context.dispatcher

  private var counter = 1

  def receive: Receive = {
    case GenerateCandidates =>
      val cancellable = context.system.scheduler.scheduleAtFixedRate(
        initialDelay = 0.milliseconds,
        interval = 150.milliseconds,
        receiver = self,
        message = GenerateCandidates
      )
      context.become(running(cancellable))
  }

  private def running(cancellable: org.apache.pekko.actor.Cancellable): Receive = {
    case GenerateCandidates if counter <= CANDIDATE_AMOUNT =>
      candidateService.generateCandidateData(counter, PARTIES_AMOUNT).flatMap { candidate =>
        Future {candidateService.upsertCandidates(List(candidate))}
      }.map { _ =>
        counter += 1
        if (counter > CANDIDATE_AMOUNT) {
          cancellable.cancel()
          promise.success(())
          self ! StopGenerating
        }
      }

    case StopGenerating =>
      println("STOPPPP")
      context.stop(self)
  }
}
