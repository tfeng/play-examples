name := "mongodb-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-plugins" % "mongodb-plugin" % "0.2.1",
  javaWs % "test",
  "me.tfeng.play-plugins" % "spring-test" % "0.2.1" % "test"
)

Avro.settings
