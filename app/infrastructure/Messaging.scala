package infrastructure

import com.typesafe.config.ConfigFactory
import akka.util.duration._
import play.libs.Akka
import akka.actor.Props
import play.api.Logger
import com.rabbitmq.client.{Connection, ConnectionFactory, QueueingConsumer}
import com.rabbitmq.client.Channel
import akka.actor.Actor

object Config {
  val RABBITMQ_HOST = ConfigFactory.load().getString("rabbitmq.host")
  val RABBITMQ_QUEUE = ConfigFactory.load().getString("rabbitmq.queue")
  val RABBITMQ_EXCHANGEE = ConfigFactory.load().getString("rabbitmq.exchange")
}

object RabbitMQConnection {

  private val connection: Connection = null

  // Return a connection if one doesn't exist. Else create a new one
  def getConnection(): Connection = {
    connection match {
      case null => {
        val factory = new ConnectionFactory()
        factory.setHost(Config.RABBITMQ_HOST)
        factory.newConnection()
      }
      case _ => connection
    }
  }
}

object Sender {

  def startSending = {
    // create the connection
    val connection = RabbitMQConnection.getConnection()
    // create the channel we use to send
    val sendingChannel = connection.createChannel()
    // make sure the queue exists we want to send to
    sendingChannel.queueDeclare(Config.RABBITMQ_QUEUE,  true, false, false, null)

    Akka.system.scheduler.schedule(2 seconds, 1 seconds
      , Akka.system.actorOf(Props(
        new SendingActor(channel = sendingChannel,
          queue = Config.RABBITMQ_QUEUE)))
      , "MSG to Queue")

    val callback1 = (x: String) => Logger.info("Recieved on queue callback 1: " + x)

    setupListener(connection.createChannel(),Config.RABBITMQ_QUEUE, callback1)

    // create an actor that starts listening on the specified queue and passes the
    // received message to the provided callback
    val callback2 = (x: String) => Logger.info("Recieved on queue callback 2: " + x)

    // setup the listener that sends to a specific queue using the SendingActor
    setupListener(connection.createChannel(),Config.RABBITMQ_QUEUE, callback2)
  }

  private def setupListener(receivingChannel: Channel, queue: String, f: (String) => Any) {
    Akka.system.scheduler.scheduleOnce(2 seconds,
      Akka.system.actorOf(Props(new ListeningActor(receivingChannel, queue, f))), "")
  }
}

class SendingActor(channel: Channel, queue: String) extends Actor {

  def receive = {
    case some: String => {
      val msg = (some + " : " + System.currentTimeMillis())
      channel.basicPublish("", queue, null, msg.getBytes())
      Logger.info(msg)
    }
    case _ => {}
  }
}


class ListeningActor(channel: Channel, queue: String, f: (String) => Any) extends Actor {

  // called on the initial run
  def receive = {
    case _ => startReceving
  }

  def startReceving = {

    val consumer = new QueueingConsumer(channel)
    channel.basicConsume(queue, true, consumer)

    while (true) {
      // wait for the message
      val delivery = consumer.nextDelivery()
      val msg = new String(delivery.getBody())

      // send the message to the provided callback function and execute this in a subactor
      context.actorOf(Props(new Actor {
        def receive = {
          case some: String => f(some)
        }
      })) ! msg
    }
  }

}