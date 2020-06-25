name := "http4s-slick"

version := "0.1"

scalaVersion := "2.13.2"

val http4sVersion = "0.21.3"
val circeVersion = "0.12.0"

// Only necessary for SNAPSHOT releases
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-yaml" % circeVersion,
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.6.4",
  "com.typesafe.slick" %% "slick" % "3.3.2",
  "c3p0" % "c3p0" % "0.9.1.2",
  "com.h2database" % "h2" % "1.4.200"
)
