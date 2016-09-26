import build._

Common.settings

name := msgpack4zJawnName

libraryDependencies ++= (
  ("org.spire-math" %% "jawn-ast" % "0.10.0") ::
  ("com.github.xuwei-k" %% "msgpack4z-core" % "0.3.5") ::
  ("org.scalacheck" %% "scalacheck" % "1.13.2" % "test") ::
  ("com.github.xuwei-k" % "msgpack4z-java" % "0.3.4" % "test") ::
  ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
  ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.1" % "test") ::
  Nil
)

Sxr.settings
