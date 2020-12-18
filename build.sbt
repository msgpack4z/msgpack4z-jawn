Common.settings

name := build.msgpack4zJawnName

libraryDependencies ++= Seq(
  "org.typelevel" %% "jawn-ast" % "1.0.3",
  "com.github.xuwei-k" %% "msgpack4z-core" % "0.5.0",
  "org.scalacheck" %% "scalacheck" % "1.15.2" % "test",
  "com.github.xuwei-k" % "msgpack4z-java" % "0.3.6" % "test",
  "com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test",
  "com.github.xuwei-k" %% "msgpack4z-native" % "0.3.6" % "test",
)

Sxr.settings
