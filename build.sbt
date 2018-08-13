import sbt._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import com.typesafe.sbt.git._

lazy val commonSettings = Seq(
  organization := "com.github.filosganga",
  scalaVersion := "2.12.6",
  libraryDependencies ++= Seq(
    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  ),
  git.remoteRepo := "origin",
  git.runner := ConsoleGitRunner,
  git.baseVersion := "0.1.0",
  git.useGitDescribe := true,
  scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-encoding",
    "utf8",
    "-Ypartial-unification"
  )
)

lazy val `saffron` = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .aggregate(core, binary, generic)
  .settings(
    name := "saffron",
    description := "native Avro serialization in scala",
    homepage := Some(url("https://github.com/ovotech/saffron")),
    organizationHomepage := Some(url("https://www.ovoenergy.com/")),
    startYear := Some(2016),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    scmInfo := Some(ScmInfo(url("https://github.com/ovotech/saffron"), "git@github.com:ovotech/saffron.git")),
    tutTargetDirectory := baseDirectory.value,
    bintrayOrganization := Some("ovotech"),
    bintrayRepository := "maven",
    bintrayPackageLabels := Seq("avro", "serialization")
  )

lazy val core: Project = (project in file("core"))
  .dependsOn(coreTestkit % Test)
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
      "org.scalacheck" %% "scalacheck" % "1.14.0",
      "org.scalatest" %% "scalatest" % "3.0.5"
    )
  )

lazy val binary: Project = (project in file("binary"))
  .dependsOn(core, coreTestkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(name := "saffron-binary")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.2.0",
      "org.apache.avro" % "avro" % "1.8.2" % Test
    )
  )

lazy val generic: Project = (project in file("generic"))
  .dependsOn(core, coreTestkit % Test)
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(name := "saffron-generic")
  .settings(
    libraryDependencies ++= Seq("com.chuusai" %% "shapeless" % "2.3.2")
  )