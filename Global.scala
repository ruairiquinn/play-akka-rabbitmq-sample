import infrastructure.Sender
import play.api._

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    Sender.startSending
  }
}

