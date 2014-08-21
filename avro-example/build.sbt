name := "avro-plugin-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies += "org.springframework" % "spring-test" % "4.0.3.RELEASE" % "test"

Avro.settings
