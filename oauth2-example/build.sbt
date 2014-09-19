name := "oauth2-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-plugins" % "oauth2-plugin" % "0.2.0-SNAPSHOT",
  javaWs % "test",
  "org.springframework" % "spring-test" % "4.0.3.RELEASE" % "test"
)

fork in Test := false
