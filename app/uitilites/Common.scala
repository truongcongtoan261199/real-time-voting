package uitilites

import org.apache.kafka.clients.producer.{Callback, RecordMetadata}

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object Common {
  val callback: Callback = new Callback {
    override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
      if (exception == null) {
        println(s"Message delivered to ${metadata.topic()} [${metadata.partition()}] at offset ${metadata.offset()}")
      } else {
        exception.printStackTrace()
      }
    }
  }

  def deliveryReport(metadata: RecordMetadata, exception: Exception): Unit = {
    if (exception != null) {
      println(s"Message delivery failed: ${exception.getMessage}")
    } else {
      println(s"Message delivered to ${metadata.topic()} [${metadata.partition()}] @ offset ${metadata.offset()}")
    }
  }

  @tailrec
  def retry[T](retryTimes: Int)(fn: => Future[T]): Future[T] = {
    Try (fn) match {
      case Success(value) => value
      case _ if retryTimes > 1 =>
        println("retrying ... ")
        retry(retryTimes-1)(fn)
      case Failure(exception) =>
        println("Failed")
        throw exception
    }
  }

}
