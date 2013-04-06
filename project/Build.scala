import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "play-akka-rabbitmq-sample"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "com.rabbitmq" % "amqp-client" % "3.0.4"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
