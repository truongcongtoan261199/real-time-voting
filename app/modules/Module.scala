package modules

import services.Impl.{CandidateServicesImpl, VoterServicesImpl}
import services.{CandidateServices, VoterServices}
import com.google.inject.AbstractModule

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[CandidateServices]).to(classOf[CandidateServicesImpl])
    bind(classOf[VoterServices]).to(classOf[VoterServicesImpl])
  }
}
