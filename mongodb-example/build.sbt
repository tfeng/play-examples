name := "mongodb-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-plugins" % "mongodb-plugin" % "0.1.9-SNAPSHOT",
  javaWs % "test",
  "org.springframework" % "spring-test" % "4.0.3.RELEASE" % "test"
)

Avro.settings

fork in Test := false
