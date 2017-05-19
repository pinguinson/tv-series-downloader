import actors.MainActor
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import http.HttpService

/**
  * Created by pinguinson on 3/29/2017.
  */
object MainApp extends App {

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system = ActorSystem("Shows")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher
  system.actorOf(Props[MainActor], "mainActor")

  val httpService = new HttpService

  Http().bindAndHandle(httpService.route, host, port)
  println(s"Listening at $host:$port")
}