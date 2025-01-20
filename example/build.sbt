import org.scalajs.linker.interface.ModuleSplitStyle

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name                                                := "laminar-html-example",
    scalaVersion                                        := "3.5.2",
    scalacOptions ++= Seq("-encoding", "utf-8", "-deprecation", "-feature"),
    scalaJSUseMainModuleInitializer                     := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("example")))
    },
    Compile / fastLinkJS / scalaJSLinkerOutputDirectory := target.value / "scalajs-modules",
    Compile / fullLinkJS / scalaJSLinkerOutputDirectory := target.value / "scalajs-modules",
    libraryDependencies ++= Seq(
      "org.scala-js"    %%% "scalajs-dom"  % "2.8.0",
      "io.github.elgca" %%% "laminar-html" % "0.1.8",
    ),
  )
