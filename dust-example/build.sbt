name := "dust-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  javaWs % "test",
  "me.tfeng.play-plugins" % "spring-test" % "0.2.10" % "test"
)

Dust.settings
