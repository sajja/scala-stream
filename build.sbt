name := "scala-stream"

description := "scala stream examples"

scalaVersion := "2.11.0"

val akkaVersion = "2.5.2"

resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.6",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
  "org.scalatest" % "scalatest_2.11" % "2.2.2" % "test"
)
