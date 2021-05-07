name := "CamelExchange"

version := "1.0"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(

  "com.typesafe.akka" %% "akka-actor" % "2.6.14",

  "com.typesafe.akka" %% "akka-remote" % "2.6.14",

  "org.apache.camel" % "camel-core" % "3.9.0",

  "org.scala-lang" % "scala-library" % "2.12.7",

  "org.apache.camel" % "camel-netty-http" % "3.9.0",

  "org.json4s" %% "json4s-native" % "3.7.0-M15",

  "org.yaml" % "snakeyaml" % "1.28",

  "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.3",

  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3"

)

mainClass in assembly := Some("com.oldtan.camel.CamelMain")
