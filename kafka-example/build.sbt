name := "kafka-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "me.tfeng.play-plugins" % "kafka-plugin" % "0.3.4",
  "org.apache.zookeeper" % "zookeeper" % "3.4.6",
  javaWs % "test",
  "me.tfeng.play-plugins" % "spring-test" % "0.3.4" % "test"
)

Avro.settings
