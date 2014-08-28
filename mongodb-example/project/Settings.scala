import sbt._
import sbt.Keys._

object Settings {
  val common: Seq[Setting[_]] = Seq(
    organization := "me.tfeng.play-plugins",
    version := "1.0.0-SNAPSHOT"
  )
}
