import sbt._, Keys._

object build extends Build {

  private val msgpack4zJawnName = "msgpack4z-jawn"
  val modules = msgpack4zJawnName :: Nil

  lazy val msgpack4z = Project("msgpack4z-jawn", file(".")).settings(
    Common.settings: _*
  ).settings(
    name := msgpack4zJawnName,
    libraryDependencies ++= (
      ("org.spire-math" %% "jawn-ast" % "0.7.2") ::
      ("com.github.xuwei-k" %% "msgpack4z-core" % "0.1.2") ::
      ("org.scalacheck" %% "scalacheck" % "1.12.2" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java07" % "0.1.3" % "test").exclude("org.msgpack", "msgpack-core") ::
      ("org.msgpack" % "msgpack-core" % "0.7.0-p8" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java06" % "0.1.0" % "test") ::
      ("com.github.xuwei-k" %% "msgpack4z-native" % "0.1.0" % "test") ::
      Nil
    )
  ).settings(
    Sxr.subProjectSxr(Compile, "classes.sxr"): _*
  )

}
