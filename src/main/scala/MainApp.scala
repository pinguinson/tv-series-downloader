import actors.MainActor
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import http.HttpService

import scala.concurrent.ExecutionContext

/**
  * Created by pinguinson on 3/29/2017.
  */
object MainApp extends App {

  implicit val system = ActorSystem("Shows")
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  system.actorOf(Props[MainActor], "mainActor")

  val httpService = new HttpService

  Http().bindAndHandle(httpService.route, "localhost", 1337)
}