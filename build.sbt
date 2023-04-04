import org.scalajs.jsenv.nodejs.NodeJSEnv
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

Global / version      := "SNAPSHOT"
Global / scalaVersion := "3.2.0"
Global / jsEnv        := new NodeJSEnv(NodeJSEnv.Config().withArgs(List("--dns-result-order=ipv4first")))

val catsCoreVersion          = "2.9.0"
val catsEffectVersion        = "3.3.0"
val catsEffectTestingVersion = "1.4.0"
val http4sVersion            = "0.23.13"
val log4catsVersion          = "2.5.0"
val redis4catsVersion        = "1.3.0"
val fs2Version               = "3.2.2"
val circeVersion             = "0.14.3"
val secureRandomVersion      = "1.0.0"
val scalaXmlVersion          = "2.1.0"
val scalaTestVersion         = "3.2.14"

val scalaJsDomVersion   = "2.4.0"
val scalaJsReactVersion = "2.1.1"
val reactVersion        = "18.2.0"
val reactKonvaVersion   = "18.2.5"

lazy val domain = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/domain"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"                     % catsCoreVersion,
      "org.typelevel" %%% "cats-effect"                   % catsEffectVersion,
      "org.typelevel" %%% "cats-effect-testing-scalatest" % catsEffectTestingVersion,
      "co.fs2"        %%% "fs2-core"                      % fs2Version,
      "co.fs2"        %%% "fs2-io"                        % fs2Version,
      "io.circe"      %%% "circe-core"                    % circeVersion,
      "io.circe"      %%% "circe-generic"                 % circeVersion,
      "io.circe"      %%% "circe-parser"                  % circeVersion,
      "org.scalatest" %%% "scalatest"                     % scalaTestVersion % Test
    )
  )

lazy val frontend = (project in file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin, ScalaJSWeb)
  .dependsOn(domain.js)
  .settings(
    scalaJSStage := (if (sys.env.get("FULL_OPT_JS").forall(_.toBoolean)) FullOptStage else FastOptStage),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    libraryDependencies ++= Seq(
      ("org.scala-js"                     %%% "scalajs-java-securerandom" % secureRandomVersion).cross(CrossVersion.for3Use2_13),
      "org.scala-js"                      %%% "scalajs-dom"               % scalaJsDomVersion,
      "com.github.japgolly.scalajs-react" %%% "core"                      % scalaJsReactVersion
    ),
    Compile / npmDependencies ++= Seq(
      "react"       -> reactVersion,
      "react-dom"   -> reactVersion,
      "react-konva" -> reactKonvaVersion
    ),
    scalaJSUseMainModuleInitializer := true
  )

lazy val backend = (project in file("modules/backend"))
  .enablePlugins(SbtWeb, WebScalaJSBundlerPlugin, JavaAppPackaging)
  .dependsOn(domain.jvm)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s"             %% "http4s-dsl"          % http4sVersion,
      "org.http4s"             %% "http4s-circe"        % http4sVersion,
      "org.http4s"             %% "http4s-blaze-server" % http4sVersion,
      "org.scala-lang.modules" %% "scala-xml"           % scalaXmlVersion,
      "dev.profunktor"         %% "redis4cats-effects"  % redis4catsVersion,
      "org.typelevel"          %% "log4cats-slf4j"      % log4catsVersion,
      "org.scalatest"          %% "scalatest"           % scalaTestVersion % Test
    ),
    scalaJSProjects := Seq(frontend),
    exportJars      := true,
    resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo),
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
    Assets / pipelineStages        := Seq(scalaJSPipeline),
    Assets / WebKeys.packagePrefix := "assets/",
    Runtime / managedClasspath += (Assets / packageBin).value,
    fork := true
  )
