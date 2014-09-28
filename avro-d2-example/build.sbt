name := "avro-d2-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  javaWs % "test",
  "me.tfeng.play-plugins" % "spring-test" % "0.2.3" % "test"
)

AvroD2.settings
