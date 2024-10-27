package uitilites

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka._
import org.apache.pekko.kafka.scaladsl.Consumer
import org.apache.pekko.stream.scaladsl.Sink
import play.api.libs.json.{JsValue, Json}

import java.util.Properties
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object KafkaConfiguration {
  private val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  // high throughput producer (at the expense of a bit of latency and CPU usage)
  props.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
  props.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
  props.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024)); // 32 KB batch size
  val producer = new KafkaProducer[String, String](props)

  val consumerProps = new Properties()
  consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "voting-group")
  consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
  consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000000)
  consumerProps.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 200000)

  val consumer = new KafkaConsumer[String, String](consumerProps)

  implicit val system: ActorSystem = ActorSystem("KafkaStreamSystem")
  implicit val ec = system.dispatcher

  val producerSettings: ProducerSettings[String, String] = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers("localhost:9092")
    .withProperty(ProducerConfig.ACKS_CONFIG, "all")
    .withProperty(ProducerConfig.LINGER_MS_CONFIG, "20")
    .withProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024))

  val consumerSettings: ConsumerSettings[String, String] = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("voting-group")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def fetchDataFromKafka(topic: String): Future[Seq[JsValue]] = {
    Consumer.plainSource(consumerSettings, Subscriptions.topics(topic))
      .map(record => Json.parse(record.value()))
      .takeWithin(10.seconds)
      .runWith(Sink.seq)
      .recoverWith {
        case e: Exception =>
          println(e.getMessage)
          Future(Seq.empty[JsValue])
      }
  }
}
