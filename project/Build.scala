import sbt._, Keys._

object build extends Build {

  private val msgpack4zJawnName = "msgpack4z-jawn"
  val modules = msgpack4zJawnName :: Nil

  lazy val msgpack4z = Project("msgpack4z-jawn", file(".")).settings(
    Common.settings: _*
  ).settings(
    name := msgpack4zJawnName,
    libraryDependencies ++= (
      ("org.spire-math" %% "jawn-ast" % "0.8.0").excludeAll( // https://github.com/non/jawn/issues/33
         ExclusionRule("org.openjdk.jmh", "jmh-core"),
         ExclusionRule("org.openjdk.jmh", "jmh-generator-bytecode"),
         ExclusionRule("org.openjdk.jmh", "jmh-generator-reflection")
      ) ::
      ("com.github.xuwei-k" %% "msgpack4z-core" % "0.1.3") ::
      ("org.scalacheck" %% "scalacheck" % "1.12.2" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java07" % "0.1.4" % "test") ::
      ("com.github.xuwei-k" % "msgpack4z-java06" % "0.1.1" % "test") ::
      ("com.github.xuwei-k" %% "msgpack4z-native" % "0.1.0" % "test") ::
      Nil
    )
  ).settings(
    Sxr.subProjectSxr(Compile, "classes.sxr"): _*
  )

}
