name := "CamelExchange"

version := "1.0"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(

  "org.apache.camel" % "camel-core" % "3.9.0",

  "org.apache.camel" % "camel-netty-http" % "3.9.0",

  "org.apache.camel" % "camel-jaxb" % "3.9.0",

  //"org.apache.camel" % "camel-undertow" % "3.9.0",
  "org.apache.camel" % "camel-xmljson" % "2.23.4",

  "xom" % "xom" % "1.3.7",

  "org.apache.camel" % "camel-jackson" % "3.9.0",

  "org.apache.camel" % "camel-jacksonxml" % "3.9.0",

  "org.json4s" %% "json4s-native" % "3.7.0-M15",

  "org.yaml" % "snakeyaml" % "1.28",

  "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.3",

  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3",

  "org.scala-lang" % "scala-library" % "2.12.7",

  "org.scala-lang.modules" %% "scala-xml" % "1.3.0"

)

mainClass in assembly := Some("com.oldtan.camel.CamelMain")
