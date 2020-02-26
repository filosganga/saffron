import sbt._
import com.typesafe.sbt.GitPlugin.autoImport._
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import com.typesafe.sbt.git._

lazy val commonSettings = Seq(
  organization := "com.github.filosganga",
  scalaVersion := "2.12.9",
  resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/",
  libraryDependencies ++= Seq(
    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
    "org.scalatest" %% "scalatest" % "3.0.8" % Test
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
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "saffron-core"
  )

lazy val binary: Project = (project in file("binary"))
  .dependsOn(core % s"$Compile->$Compile;$Test->$Test")
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(name := "saffron-binary")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.6.1",
      "org.scodec" %% "scodec-core" % "1.11.4",
      "org.scodec" %% "scodec-bits" % "1.11.12",
      "org.apache.avro" % "avro" % "1.9.0" % Test
    )
  )

lazy val generic: Project = (project in file("generic"))
  .dependsOn(core % s"$Compile->$Compile;$Test->$Test")
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(name := "saffron-generic")
  .settings(
    libraryDependencies ++= Seq("com.chuusai" %% "shapeless" % "2.3.3")
  )