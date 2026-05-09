import sbtrelease.Git
import sbtrelease.ReleaseStateTransformations._

val msgpack4zJawnName = "msgpack4z-jawn"
val modules = msgpack4zJawnName :: Nil

def gitHash(): String = sys.process.Process("git rev-parse HEAD").lazyLines_!.head

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
}
val tagOrHash = Def.setting {
  if (isSnapshot.value) gitHash() else tagName.value
}

val unusedWarnings = Def.setting(
  scalaBinaryVersion.value match {
    case "2.12" =>
      Seq(
        "-Ywarn-unused",
      )
    case _ =>
      Seq(
        "-Wunused:imports",
      )
  }
)

val scalaVersions = Seq("2.12.21", "2.13.18", "3.3.7")

val commonSettings = Def.settings(
  ReleasePlugin.extraReleaseCommands,
  Test / testOptions += Tests.Argument(
    TestFrameworks.ScalaCheck,
    "-minSuccessfulTests",
    "300"
  ),
  commands += Command.command("updateReadme")(updateReadmeTask),
  publishTo := (if (isSnapshot.value) None else localStaging.value),
  releaseTagName := tagName.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    updateReadmeProcess,
    tagRelease,
    releaseStepCommandAndRemaining("publishSigned"),
    releaseStepCommandAndRemaining("sonaRelease"),
    setNextVersion,
    commitNextVersion,
    updateReadmeProcess,
    pushChanges
  ),
  organization := "com.github.xuwei-k",
  homepage := Some(url("https://github.com/msgpack4z")),
  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
  scalacOptions ++= Seq(
    "-release:8",
    "-deprecation",
    "-unchecked",
    "-language:existentials",
    "-language:implicitConversions",
  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        Seq("-Xlint")
      case _ =>
        Nil
    }
  },
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq("-Xfuture")
      case _ =>
        Nil
    }
  },
  scalacOptions ++= unusedWarnings.value,
  (Compile / doc / scalacOptions) ++= {
    Seq(
      "-sourcepath",
      (LocalRootProject / baseDirectory).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/msgpack4z/msgpack4z-jawn/tree/${tagOrHash.value}€{FILE_PATH}.scala"
    )
  },
  pomExtra :=
    <developers>
      <developer>
        <id>xuwei-k</id>
        <name>Kenji Yoshida</name>
        <url>https://github.com/xuwei-k</url>
      </developer>
    </developers>
    <scm>
      <url>git@github.com:msgpack4z/msgpack4z-jawn.git</url>
      <connection>scm:git:git@github.com:msgpack4z/msgpack4z-jawn.git</connection>
      <tag>{tagOrHash.value}</tag>
    </scm>,
  description := "msgpack4z jawn binding",
  pomPostProcess := { node =>
    import scala.xml._
    import scala.xml.transform._
    def stripIf(f: Node => Boolean) = new RewriteRule {
      override def transform(n: Node) =
        if (f(n)) NodeSeq.Empty else n
    }
    val stripTestScope = stripIf { n =>
      n.label == "dependency" && (n \ "scope").text == "test"
    }
    new RuleTransformer(stripTestScope).transform(node)(0)
  },
  Seq(Compile, Test).flatMap(c => c / console / scalacOptions --= unusedWarnings.value),
)

val msgpack4zJawn = projectMatrix
  .defaultAxes()
  .withId("msgpack4z-jawn")
  .in(file("."))
  .settings(
    commonSettings,
    name := msgpack4zJawnName,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-ast" % "1.6.0",
      "com.github.xuwei-k" %% "msgpack4z-core" % "0.6.2",
      "org.scalacheck" %% "scalacheck" % "1.19.0" % "test",
      "com.github.xuwei-k" % "msgpack4z-java" % "0.4.0" % "test",
      "com.github.xuwei-k" % "msgpack4z-java06" % "0.2.0" % "test",
      "com.github.xuwei-k" %% "msgpack4z-native" % "0.4.0" % "test",
    ),
  )
  .jvmPlatform(
    scalaVersions,
    Def.settings()
  )
  .jsPlatform(
    Nil,
    Def.settings(
      scalacOptions += {
        val a = (LocalRootProject / baseDirectory).value.toURI.toString
        val g = "https://raw.githubusercontent.com/msgpack4z/msgpack4z-jawn/" + gitHash()
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, _)) =>
            s"-P:scalajs:mapSourceURI:$a->$g/"
          case _ =>
            s"-scalajs-mapSourceURI:$a->$g/"
        }
      },
    )
  )
  .nativePlatform(
    scalaVersions,
    Def.settings(
      evictionErrorLevel := Level.Warn,
    )
  )

val root = rootProject.autoAggregate.settings(
  commonSettings,
  autoScalaLibrary := false,
  PgpKeys.publishLocalSigned := {},
  PgpKeys.publishSigned := {},
  publishLocal := {},
  publish := {},
  Compile / publishArtifact := false,
  Compile / scalaSource := (LocalRootProject / baseDirectory).value / "dummy",
  Test / scalaSource := (LocalRootProject / baseDirectory).value / "dummy",
)

val updateReadmeTask: State => State = { state =>
  val extracted = Project.extract(state)
  val v = extracted.get(version)
  val org = extracted.get(organization)
  val snapshotOrRelease = if (extracted.get(isSnapshot)) "snapshots" else "releases"
  val readme = "README.md"
  val readmeFile = file(readme)
  val newReadme = IO
    .readLines(readmeFile)
    .map { line =>
      val matchReleaseOrSnapshot = line.contains("SNAPSHOT") == v.contains("SNAPSHOT")
      def n = modules(modules.indexWhere(line.contains))
      if (line.startsWith("libraryDependencies") && matchReleaseOrSnapshot) {
        s"""libraryDependencies += "${org}" %% "$n" % "$v""""
      } else line
    }
    .mkString("", "\n", "\n")
  IO.write(readmeFile, newReadme)
  val git = new Git(extracted.get(baseDirectory))
  git.add(readme) ! state.log
  git.commit(message = "update " + readme, sign = false, signOff = false) ! state.log
  sys.process.Process("git diff HEAD^") ! state.log
  state
}

val updateReadmeProcess: ReleaseStep = updateReadmeTask
