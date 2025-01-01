val commonScalacOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:implicitConversions",
  "-new-syntax",
  "-explain",
  "-Yexplicit-nulls",
  "-rewrite",
  "-source:3.7",
)

lazy val jsSettings = Seq(
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
  scalaModuleInfo ~= (_.map(_.withOverrideScalaVersion(true))),
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "2.8.0",
    "com.raquo"    %%% "laminar"     % "17.2.0",
  ),
  // testing
  libraryDependencies += "com.lihaoyi" %%% "utest" % "0.8.1" % "test",
  testFrameworks += new TestFramework("utest.runner.Framework"),
)

def buildinfo_settings(pkg: String) =
  Seq(
//    buildInfoKeys    := Seq[BuildInfoKey](
//      name,
//      version,
//      scalaVersion,
//      sbtVersion,
//      isSnapshot,
//    ),
//    buildInfoObject  := "BuildInfo",
//    buildInfoPackage := pkg,
  )

lazy val compilerSettings = Seq(
  // not sure what this does anymore so removed it
  // (doc / Compile / scalacOptions) ++= Seq("-groups"),
  scalacOptions ++= commonScalacOptions,
  autoAPIMappings     := true,
  autoCompilerPlugins := true,
)

lazy val resolverSettings = Seq(resolvers ++= Seq())

lazy val publishSettings = Seq()

def std_settings(p: String, d: String) =
  Seq(
    name         := p,
    organization := "io.github.elgca",
    description  := d,
    libraryDependencies ++= Seq(
      // "org.scalatest" %%% "scalatest" % "3.2.0-M2" % Test
    ),
  ) ++ resolverSettings ++ compilerSettings ++ jsSettings ++ publishSettings

inThisBuild(List(scalaVersion := "3.5.2", startYear := Some(2024)))

lazy val root = (project in file("."))
  .settings(publish / skip := true)
  .aggregate(`laminar-html`)

lazy val `laminar-html` = project
  .settings(std_settings("laminar-html", "laminar-html"))
  .settings(buildinfo_settings("html"))
  .enablePlugins(ScalaJSPlugin)
