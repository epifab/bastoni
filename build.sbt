import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}
import org.scalajs.jsenv.nodejs.NodeJSEnv

Global / version := "SNAPSHOT"
Global / scalaVersion := "3.1.0"
Global / jsEnv := new NodeJSEnv(NodeJSEnv.Config().withArgs(List("--dns-result-order=ipv4first")))

val catsCoreVersion = "2.6.1"
val catsEffectVersion = "3.3.0"
val catsEffectTestingVersion = "1.4.0"
val log4catsVersion = "2.1.1"
val redis4catsVersion = "1.0.0"
val fs2Version = "3.2.2"
val circeVersion = "0.14.1"
val scalaTestVersion = "3.2.10"

lazy val domain = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/domain"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"  %%% "cats-core"                     % catsCoreVersion,
      "org.typelevel"  %%% "cats-effect"                   % catsEffectVersion,
      "org.typelevel"  %%% "cats-effect-testing-scalatest" % catsEffectTestingVersion,
      "co.fs2"         %%% "fs2-core"                      % fs2Version,
      "co.fs2"         %%% "fs2-io"                        % fs2Version,
      "io.circe"       %%% "circe-core"                    % circeVersion,
      "io.circe"       %%% "circe-generic"                 % circeVersion,
      "io.circe"       %%% "circe-parser"                  % circeVersion,
      "org.scalatest"  %%% "scalatest"                     % scalaTestVersion % Test
    )
  )

lazy val backend = (project in file("modules/backend"))
  .enablePlugins(SbtWeb, JavaAppPackaging)
  .dependsOn(domain.jvm)
  .settings(
    libraryDependencies ++= Seq(
      "dev.profunktor" %% "redis4cats-effects" % redis4catsVersion,
      "org.typelevel"  %% "log4cats-slf4j"     % log4catsVersion,
      "org.scalatest"  %% "scalatest"          % scalaTestVersion % Test
    ),
    scalaJSProjects := Seq(frontend),
    exportJars := true,

    resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo),
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),

    Assets / pipelineStages := Seq(scalaJSPipeline),
    Assets / WebKeys.packagePrefix := "assets/",
    Runtime / managedClasspath += (Assets / packageBin).value
  )

lazy val frontend = (project in file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(domain.js)
  .settings(
    scalaJSUseMainModuleInitializer := true
  )
