addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.0.15")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.5")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Yno-adapted-args",
)

fullResolvers ~= { _.filterNot(_.name == "jcenter") }
