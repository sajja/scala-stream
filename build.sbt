name := "scala-stream"

description := "scala stream examples"

scalaVersion := "2.11.0"

val akkaVersion = "2.5.2"
val sprayVersion = "1.3.2"
resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.6",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
  "com.typesafe.akka" %% "akka-http" % "10.0.6",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion, // or whatever the latest version is
  "io.spray" %% "spray-client" % sprayVersion,
  "io.spray" %% "spray-can" % sprayVersion,
  "io.spray" %% "spray-routing" % sprayVersion,
  "io.spray" %% "spray-json" % "1.3.1",
  "org.jsoup" % "jsoup" % "1.8.1",
  "io.spray" %% "spray-testkit" % sprayVersion % "test",
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "com.googlecode.flyway" % "flyway-core" % "2.1.1",
  "c3p0" % "c3p0" % "0.9.0.4",
  "org.scalatest" % "scalatest_2.11" % "2.2.2" % "test"
)
