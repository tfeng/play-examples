name := "avro-d2-plugin-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  javaWs % "test",
  "org.springframework" % "spring-test" % "4.0.3.RELEASE" % "test"
)

AvroD2.settings

fork in Test := false
