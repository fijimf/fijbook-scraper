val Http4sVersion = "0.20.9"
val CirceVersion = "0.12.0"
val DoobieVersion = "0.7.0"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val TypesafeConfVersion = "1.3.4"
val FlywayVersion = "6.0.3"


enablePlugins(BuildInfoPlugin)
enablePlugins(JavaAppPackaging)

lazy val root = (project in file("."))
  .settings(
    organization := "com.fijimf.deepfij",
    name := "fijbook-scraper",
    version := "1.0.0",
    scalaVersion := "2.12.8",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, buildInfoBuildNumber),
    buildInfoPackage := "com.fijimf.deepfij.scraping",
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,

    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "io.circe"        %% "circe-parser"        % CirceVersion,
      "io.circe"        %% "circe-optics"        % CirceVersion,
      "org.tpolecat"    %% "doobie-core"         % DoobieVersion,
      "org.tpolecat"    %% "doobie-postgres"     % DoobieVersion,
      "org.tpolecat"    %% "doobie-hikari"       % DoobieVersion,
      "org.tpolecat"    %% "doobie-specs2"       % DoobieVersion,
      "org.tpolecat"    %% "doobie-scalatest"    % DoobieVersion,
      "org.specs2"      %% "specs2-core"         % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "com.typesafe"    %  "config"              % TypesafeConfVersion,
      "org.flywaydb"    % "flyway-core"          % FlywayVersion,
      "commons-codec"   % "commons-codec"        % "1.13",
      "com.spotify"     % "docker-client"        % "8.14.0",
      "it.sauronsoftware.cron4j" % "cron4j"      % "2.2.5",
      "com.amazonaws"   % "aws-java-sdk-s3"      % "1.11.651",
      "org.ccil.cowan.tagsoup" %       "tagsoup" % "1.2.1",

      "com.fijimf.deepfij" %% "fijbook-schedule" % "1.0.2g"
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0"),
    fork in ThisBuild in Test:= false,
    parallelExecution in ThisBuild in Test := false
  )

enablePlugins(JavaAppPackaging)

maintainer in Docker := "Jim Frohnhofer <fijimf@gmail.com>"
packageSummary in Docker := "REST microservice to handle schedule operations"
packageDescription := "REST microservice to handle schedule operations"

//wartremoverWarnings ++= Warts.allBut(Warts.unsafe:_*)
wartremoverErrors ++= Warts.unsafe.filter(_ == Wart.ImplicitParameter)
scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings",
)
