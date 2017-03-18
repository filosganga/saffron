import sbt._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import com.typesafe.sbt.git._
import de.heikoseeberger.sbtheader.license.Apache2_0

lazy val commonSettings = Seq(
  organization := "com.github.ovotech",

  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.12.1", "2.11.8"),

  libraryDependencies ++= Seq(
    "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
    "org.scalatest" %% "scalatest" % "3.0.1" % Test
  ),

  headers := Map(
    "java" -> Apache2_0("2016", "OVO Energy"),
    "proto" -> Apache2_0("2016", "OVO Energy", "//"),
    "scala" -> Apache2_0("2016", "OVO Energy"),
    "conf" -> Apache2_0("2016", "OVO Energy", "#")
  ),

  git.remoteRepo := "origin",
  git.runner := ConsoleGitRunner,
  git.baseVersion := "0.1.0",
  git.useGitDescribe := true
)

lazy val `saffron` = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .aggregate(core, binary)
  .settings(

    name := "saffron",
    description := "native Avro serialization in scala",

    homepage := Some(url("https://github.com/ovotech/saffron")),
    organizationHomepage := Some(url("https://www.ovoenergy.com/")),
    startYear := Some(2016),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),

    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ovotech/saffron"),
        "git@github.com:ovotech/saffron.git"
      )
    ),

    tutSettings,
    tutTargetDirectory := baseDirectory.value,
    bintrayOrganization := Some("ovotech"),
    bintrayRepository := "maven",
    bintrayPackageLabels := Seq("avro", "serialization")
  )

lazy val core: Project = (project in file("core"))
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "saffron-core",
    // Look at http://stackoverflow.com/a/26562928/462152
    internalDependencyClasspath in Test ++= {
      (exportedProducts in Compile in LocalProject("coreTestkit")).value
    }
  )

lazy val coreTestkit: Project = (project in file("core-testkit"))
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "saffron-core-testkit",
    // Look at http://stackoverflow.com/a/26562928/462152
    internalDependencyClasspath in Compile ++= {
      (exportedProducts in Compile in LocalProject("core")).value
    },
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.13.4",
      "org.scalatest" %% "scalatest" % "3.0.1"
    )
  )

lazy val binary: Project = (project in file("binary"))
  .dependsOn(core, coreTestkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "saffron-binary"
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "0.9.0",
      "org.apache.avro" % "avro" % "1.8.1" % Test
    )
  )