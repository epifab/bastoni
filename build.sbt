// Global / name := "bastoni"
Global / version := "SNAPSHOT"
Global / scalaVersion := "3.1.0"
// cleanGlobal / scalacOptions ++= Seq("-Xmax-inlines", "128")

val catsCoreVersion = "2.6.1"
val catsEffectVersion = "3.2.9"
val log4catsVersion = "2.1.1"
val redis4catsVersion = "1.0.0"
val fs2Version = "3.2.2"
val circeVersion = "0.14.1"
val scalaTestVersion = "3.2.9"

lazy val domain = (project in file("modules/domain"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"  %% "cats-core"      % catsCoreVersion,
      "org.typelevel"  %% "cats-effect"    % catsEffectVersion,
      "co.fs2"         %% "fs2-core"       % fs2Version,
      "co.fs2"         %% "fs2-io"         % fs2Version,
      "io.circe"       %% "circe-core"     % circeVersion,
      "io.circe"       %% "circe-generic"  % circeVersion,
      "io.circe"       %% "circe-parser"   % circeVersion,
      "org.scalatest"  %% "scalatest"      % scalaTestVersion % Test
    )
  )

lazy val backend = (project in file("modules/backend"))
  .dependsOn(domain)
  .settings(
    libraryDependencies ++= Seq(
      "dev.profunktor" %% "redis4cats-effects" % redis4catsVersion,
      "org.typelevel"  %% "log4cats-slf4j"     % log4catsVersion,
      "org.scalatest"  %% "scalatest"          % scalaTestVersion % Test
    )
  )
