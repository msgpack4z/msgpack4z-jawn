import sbt._, Keys._

object build extends Build {

  private val msgpack4zJawnName = "msgpack4z-jawn"
  val modules = msgpack4zJawnName :: Nil

  lazy val msgpack4z = Project("msgpack4z-jawn", file(".")).settings(
    Common.settings: _*
  ).settings(
    name := msgpack4zJawnName,
    libraryDependencies ++= (
      ("org.spire-math" %% "jawn-ast" % "0.8.3") ::
      ("com.github.xuwei-k" %% "msgpack4z-core" % "0.3.0") ::
      ("org.scalacheck" %% "scalacheck" % "1.12.5" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java07" % "0.2.0" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
      ("com.github.xuwei-k" %% "msgpack4z-native" % "0.2.0" % "test") ::
      Nil
    )
  ).settings(
    Sxr.subProjectSxr(Compile, "classes.sxr"): _*
  )

}
