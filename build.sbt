name := "akka-shows-downloader"

version := "1.0"

scalaVersion := "2.12.1"

mainClass in assembly := Some("MainApp")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.1",
  "com.typesafe.akka" %% "akka-http" % "10.0.6",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.6",
  "com.typesafe.akka" %% "akka-http-jackson" % "10.0.6",
  "net.ruippeixotog" %% "scala-scraper" % "1.2.0",
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0",
  "org.postgresql" % "postgresql" % "9.4-1206-jdbc42",
  "io.spray" %% "spray-json" % "1.3.3",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "com.github.nscala-time" %% "nscala-time" % "2.16.0",
  "ch.megard" %% "akka-http-cors" % "0.2.1"
)