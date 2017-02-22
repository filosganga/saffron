import sbt._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import com.typesafe.sbt.git._
import de.heikoseeberger.sbtheader.license.Apache2_0

lazy val `arvo` = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(

    organization := "com.github.ovotech",
    name := "arvo",
    description := "native Avro serialization in scala",

    homepage := Some(url("https://github.com/ovotech/akka-persistence-query-view")),
    organizationHomepage := Some(url("https://www.ovoenergy.com/")),
    startYear := Some(2016),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),

    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ovotech/arvo"),
        "git@github.com:ovotech/arvo.git"
      )
    ),

    git.remoteRepo := "origin",
    git.runner := ConsoleGitRunner,
    git.baseVersion := "0.1.0",
    git.useGitDescribe := true,


    scalaVersion := "2.12.1",
    crossScalaVersions := Seq("2.12.1", "2.11.8"),

    libraryDependencies ++= Seq(
      "org.apache.avro" % "avro" % "1.8.1" % Test,
      "org.scalacheck" % "scalacheck_2.12" % "1.13.4" % Test,
      "org.scalatest" % "scalatest_2.12" % "3.0.1" % Test
    ),

    headers := Map(
      "java" -> Apache2_0("2016", "OVO Energy"),
      "proto" -> Apache2_0("2016", "OVO Energy", "//"),
      "scala" -> Apache2_0("2016", "OVO Energy"),
      "conf" -> Apache2_0("2016", "OVO Energy", "#")
    ),
    tutSettings,
    tutTargetDirectory := baseDirectory.value,
    bintrayOrganization := Some("ovotech"),
    bintrayRepository := "maven",
    bintrayPackageLabels := Seq("akka", "akka-persistence", "event-sourcing", "cqrs")
  )