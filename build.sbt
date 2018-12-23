Common.settings

name := build.msgpack4zJawnName

libraryDependencies ++= (
  ("org.typelevel" %% "jawn-ast" % "0.14.0") ::
  ("com.github.xuwei-k" %% "msgpack4z-core" % "0.3.9") ::
  ("org.scalacheck" %% "scalacheck" % "1.14.0" % "test") ::
  ("com.github.xuwei-k" % "msgpack4z-java" % "0.3.5" % "test") ::
  ("com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test") ::
  ("com.github.xuwei-k" %% "msgpack4z-native" % "0.3.5" % "test") ::
  Nil
)

Sxr.settings
