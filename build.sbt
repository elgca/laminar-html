val commonScalacOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:implicitConversions",
  "-experimental",
  "-new-syntax",
  "-explain",
  "-Yexplicit-nulls",
  "-rewrite",
  "-source:3.7",
  "-Wnonunit-statement", // 强制不能为空
)

lazy val jsSettings = Seq(
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
  scalaModuleInfo ~= (_.map(_.withOverrideScalaVersion(true))),
  libraryDependencies ++= Seq(
    "org.scala-js"           %%% "scalajs-dom"              % "2.8.0",
    "com.raquo"              %%% "laminar"                  % "17.2.0",
    "com.lihaoyi"            %%% "sourcecode"               % "0.4.3-M5",
    "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.4.0",
  ),
  // testing
  libraryDependencies += "com.lihaoyi" %%% "utest" % "0.8.1" % "test",
  testFrameworks += new TestFramework("utest.runner.Framework"),
)

def buildinfo_settings(pkg: String) =
  Seq()

lazy val compilerSettings = Seq(
  // not sure what this does anymore so removed it
  // (doc / Compile / scalacOptions) ++= Seq("-groups"),
  scalacOptions ++= commonScalacOptions,
  autoAPIMappings     := true,
  autoCompilerPlugins := true,
)

lazy val resolverSettings = Seq(resolvers ++= Seq())

ThisBuild / scalaVersion := "3.5.2"
ThisBuild / startYear    := Some(2024)
ThisBuild / organization := "io.github.elgca"

def std_settings(p: String, d: String) =
  Seq(
    name        := p,
    description := d,
    libraryDependencies ++= Seq(
      // "org.scalatest" %%% "scalatest" % "3.2.0-M2" % Test
    ),
  ) ++ resolverSettings ++ compilerSettings ++ jsSettings

lazy val root = (project in file("."))
  .settings(publish / skip := true)
  .aggregate(`laminar-html`)

lazy val `laminar-html` = project
  .settings(std_settings("laminar-html", "laminar-html"))
  .settings(buildinfo_settings("html"))
  .enablePlugins(ScalaJSPlugin)

// 显示类型提示
val _ = {
  System.setProperty("show_type_hints", "true")
  System.setProperty("strict_event_function", "false")
}
