name := "schema-evolution-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "com.google.guava" % "guava" % "18.0",
  javaWs % "test",
  "me.tfeng.play-plugins" % "spring-test" % "0.4.0" % "test"
)

unmanagedResourceDirectories in Compile <+= baseDirectory / "protocols"

AvroD2.settings
