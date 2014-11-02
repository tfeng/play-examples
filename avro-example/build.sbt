name := "avro-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies += "me.tfeng.play-plugins" % "spring-test" % "0.3.2" % "test"

Avro.settings
