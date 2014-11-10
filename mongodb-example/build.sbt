name := "mongodb-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-plugins" % "mongodb-plugin" % "0.3.3",
  javaWs % "test",
  "me.tfeng.play-plugins" % "spring-test" % "0.3.3" % "test"
)

Avro.settings
