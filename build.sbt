import org.scalajs.jsenv.nodejs.NodeJSEnv
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

Global / version      := "SNAPSHOT"
Global / scalaVersion := "3.3.1"
Global / jsEnv        := new NodeJSEnv(NodeJSEnv.Config().withArgs(List("--dns-result-order=ipv4first")))

val catsCoreVersion          = "2.9.0"
val catsEffectVersion        = "3.3.0"
val catsEffectTestingVersion = "1.5.0"
val http4sVersion            = "0.23.19"
val http4sBlazeVersion       = "0.23.14"
val http4sJdkClientVersion   = "0.9.1"
val log4catsVersion          = "2.6.0"
val logbackClassicVersion    = "1.4.12"
val redis4catsVersion        = "1.4.1"
val fs2Version               = "3.2.2"
val circeVersion             = "0.14.5"
val secureRandomVersion      = "1.0.0"
val scalaXmlVersion          = "2.1.0"
val scalaTestVersion         = "3.2.15"

val scalaJsDomVersion   = "2.8.0"
val scalaJsReactVersion = "2.1.1"
val konvaVersion        = "9.3.3"
val canvasVersion       = "2.11.2"
val reactVersion        = "18.2.0"
val reactKonvaVersion   = "18.2.10"

lazy val domain = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/domain"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"                     % catsCoreVersion,
      "org.typelevel" %%% "cats-effect"                   % catsEffectVersion,
      "co.fs2"        %%% "fs2-core"                      % fs2Version,
      "co.fs2"        %%% "fs2-io"                        % fs2Version,
      "io.circe"      %%% "circe-core"                    % circeVersion,
      "io.circe"      %%% "circe-generic"                 % circeVersion,
      "io.circe"      %%% "circe-parser"                  % circeVersion,
      "org.typelevel" %%% "log4cats-core"                 % log4catsVersion,
      "org.typelevel"  %% "log4cats-slf4j"                % log4catsVersion,
      "ch.qos.logback"  % "logback-classic"               % logbackClassicVersion,
      "org.typelevel" %%% "cats-effect-testing-scalatest" % catsEffectTestingVersion % Test,
      "org.scalatest" %%% "scalatest"                     % scalaTestVersion         % Test
    )
  )

lazy val frontend = (project in file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin, ScalaJSWeb)
  .dependsOn(domain.js)
  .settings(
    scalaJSStage := (if (sys.env.get("FULL_OPT_JS").forall(_.toBoolean)) FullOptStage else FastOptStage),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    libraryDependencies ++= Seq(
      ("org.scala-js" %%% "scalajs-java-securerandom" % secureRandomVersion).cross(CrossVersion.for3Use2_13),
      "org.scala-js"  %%% "scalajs-dom"               % scalaJsDomVersion,
      "com.github.japgolly.scalajs-react" %%% "core" % scalaJsReactVersion
    ),
    Compile / npmDependencies ++= Seq(
      "react"       -> reactVersion,
      "react-dom"   -> reactVersion,
      "konva"       -> konvaVersion,
      "react-konva" -> reactKonvaVersion
    ),
    scalaJSUseMainModuleInitializer := true
  )

lazy val backend = (project in file("modules/backend"))
  .enablePlugins(SbtWeb, WebScalaJSBundlerPlugin, JavaAppPackaging)
  .dependsOn(domain.jvm)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s"             %% "http4s-dsl"                    % http4sVersion,
      "org.http4s"             %% "http4s-circe"                  % http4sVersion,
      "org.http4s"             %% "http4s-blaze-server"           % http4sBlazeVersion,
      "org.http4s"             %% "http4s-jdk-http-client"        % http4sJdkClientVersion,
      "org.scala-lang.modules" %% "scala-xml"                     % scalaXmlVersion,
      "dev.profunktor"         %% "redis4cats-effects"            % redis4catsVersion,
      "org.scalatest"          %% "scalatest"                     % scalaTestVersion         % Test,
      "org.typelevel"          %% "cats-effect-testing-scalatest" % catsEffectTestingVersion % Test
    ),
    scalaJSProjects := Seq(), // frontend),
    exportJars      := true,
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
    Assets / pipelineStages        := Seq(scalaJSPipeline),
    Assets / WebKeys.packagePrefix := "assets/",
    Runtime / managedClasspath += (Assets / packageBin).value,
    fork := true
  )

lazy val root = (project in file(".")).aggregate(domain.jvm, backend)
