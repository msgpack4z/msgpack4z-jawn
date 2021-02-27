import sbt._, Keys._
import sbtrelease._
import sbtrelease.ReleasePlugin.autoImport._
import ReleaseStateTransformations._
import com.jsuereth.sbtpgp.PgpKeys
import xerial.sbt.Sonatype.autoImport._

object Common {

  private def gitHash: String =
    sys.process.Process("git rev-parse HEAD").lineStream_!.head

  private[this] val unusedWarnings = Seq(
    "-Ywarn-unused",
  )

  private[this] val Scala212 = "2.12.13"

  val settings = Seq[SettingsDefinition](
    ReleasePlugin.extraReleaseCommands,
    fullResolvers ~= { _.filterNot(_.name == "jcenter") },
    Test / testOptions += Tests.Argument(
      TestFrameworks.ScalaCheck,
      "-minSuccessfulTests",
      "300"
    ),
    commands += Command.command("updateReadme")(UpdateReadme.updateReadmeTask),
    publishTo := sonatypePublishToBundle.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      UpdateReadme.updateReadmeProcess,
      tagRelease,
      ReleaseStep(
        action = { state =>
          Project.extract(state).runTask(PgpKeys.publishSigned, state)._1
        },
        enableCrossBuild = true
      ),
      releaseStepCommand("sonatypeBundleRelease"),
      setNextVersion,
      commitNextVersion,
      UpdateReadme.updateReadmeProcess,
      pushChanges
    ),
    credentials ++= PartialFunction
      .condOpt(sys.env.get("SONATYPE_USER") -> sys.env.get("SONATYPE_PASS")) { case (Some(user), Some(pass)) =>
        Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
      }
      .toList,
    organization := "com.github.xuwei-k",
    homepage := Some(url("https://github.com/msgpack4z")),
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-deprecation",
      "-unchecked",
      "-Xlint",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
    ),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq("-Xfuture")
        case _ =>
          Nil
      }
    },
    scalacOptions ++= unusedWarnings,
    scalaVersion := Scala212,
    crossScalaVersions := Scala212 :: "2.13.5" :: Nil,
    (Compile / doc / scalacOptions) ++= {
      val tag =
        if (isSnapshot.value) gitHash
        else {
          "v" + version.value
        }
      Seq(
        "-sourcepath",
        (LocalRootProject / baseDirectory).value.getAbsolutePath,
        "-doc-source-url",
        s"https://github.com/msgpack4z/msgpack4z-jawn/tree/${tag}€{FILE_PATH}.scala"
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
        <tag>{
        if (isSnapshot.value) gitHash
        else {
          "v" + version.value
        }
      }</tag>
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
    Seq(Compile, Test).flatMap(c => c / console / scalacOptions --= unusedWarnings)
  ).flatMap(_.settings)

}
