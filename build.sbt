Common.settings

name := build.msgpack4zJawnName

libraryDependencies ++= (
  ("org.spire-math" %% "jawn-ast" % "0.11.1") ::
  ("com.github.xuwei-k" %% "msgpack4z-core" % "0.3.8") ::
  ("org.scalacheck" %% "scalacheck" % "1.13.5" % "test") ::
  ("com.github.xuwei-k" % "msgpack4z-java" % "0.3.5" % "test") ::
  ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
  ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.4" % "test") ::
  Nil
)

Sxr.settings
