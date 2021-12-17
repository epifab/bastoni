import org.scalajs.jsenv.nodejs.NodeJSEnv
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

Global / version := "SNAPSHOT"
Global / scalaVersion := "3.1.0"
Global / jsEnv := new NodeJSEnv(NodeJSEnv.Config().withArgs(List("--dns-result-order=ipv4first")))
Global / scalaJSStage := (if (sys.env.get("FULL_OPT_JS").forall(_.toBoolean)) FullOptStage else FastOptStage)

val catsCoreVersion = "2.6.1"
val catsEffectVersion = "3.3.0"
val catsEffectTestingVersion = "1.4.0"
val http4sVersion = "0.23.6"
val http4sJdkClientVersion = "0.5.0"
val log4catsVersion = "2.1.1"
val redis4catsVersion = "1.0.0"
val fs2Version = "3.2.2"
val circeVersion = "0.14.1"
val scalaXmlVersion = "2.0.1"
val scalaTestVersion = "3.2.10"

val scalaJsDomVersion = "2.0.0"
val scalaJsReactVersion = "2.0.0"
val reactVersion = "17.0.2"

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

lazy val frontend = (project in file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin, JSDependenciesPlugin, ScalaJSWeb)
  .dependsOn(domain.js)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js"                      %%% "scalajs-dom" % scalaJsDomVersion,
      "com.github.japgolly.scalajs-react" %%% "core"        % scalaJsReactVersion,
      "com.github.japgolly.scalajs-react" %%% "extra"       % scalaJsReactVersion,
      "com.github.japgolly.scalajs-react" %%% "test"        % scalaJsReactVersion % Test
    ),
    jsDependencies ++= Seq(
      "org.webjars.npm" % "react" % reactVersion

        /        "umd/react.development.js"
        minified "umd/react.production.min.js"
        commonJSName "React",

      "org.webjars.npm" % "react-dom" % reactVersion

        /            "umd/react-dom.development.js"
        minified     "umd/react-dom.production.min.js"
        dependsOn    "umd/react.development.js"
        commonJSName "ReactDOM",

      "org.webjars.npm" % "react-dom" % reactVersion
        /            "umd/react-dom-server.browser.development.js"
        minified     "umd/react-dom-server.browser.production.min.js"
        dependsOn    "umd/react-dom.development.js"
        commonJSName "ReactDOMServer",

      "org.webjars.npm" % "react-dom" % reactVersion % Test
        /            "umd/react-dom-test-utils.development.js"
        minified     "umd/react-dom-test-utils.production.min.js"
        dependsOn    "umd/react-dom.development.js"
        commonJSName "ReactTestUtils",

      "org.webjars.npm" % "js-cookie" % "2.2.1"
        /            "js.cookie.js"
        commonJSName "Cookie"
    ),
    scalaJSUseMainModuleInitializer := true,
    packageJSDependencies / skip := false
  )

lazy val backend = (project in file("modules/backend"))
  .enablePlugins(SbtWeb, JavaAppPackaging)
  .dependsOn(domain.jvm)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s"             %% "http4s-dsl"             % http4sVersion,
      "org.http4s"             %% "http4s-circe"           % http4sVersion,
      "org.http4s"             %% "http4s-blaze-server"    % http4sVersion,
      "org.http4s"             %% "http4s-jdk-http-client" % http4sJdkClientVersion % Test,
      "org.scala-lang.modules" %% "scala-xml"              % scalaXmlVersion,
      "dev.profunktor"         %% "redis4cats-effects"     % redis4catsVersion,
      "org.typelevel"          %% "log4cats-slf4j"         % log4catsVersion,
      "org.scalatest"          %% "scalatest"              % scalaTestVersion % Test
    ),
    scalaJSProjects := Seq(frontend),
    exportJars := true,

    resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo),
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),

    Assets / pipelineStages := Seq(scalaJSPipeline),
    Assets / WebKeys.packagePrefix := "assets/",
    Assets / managedResources += (frontend / Compile / packageJSDependencies).value,
    Runtime / managedClasspath += (Assets / packageBin).value,

    fork := true
  )
