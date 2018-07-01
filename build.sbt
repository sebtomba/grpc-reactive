
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

val additionalScalacOptions = Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-infer-any",
  "-Ywarn-unused-import",
  "-Ypartial-unification",
//  "-Xfatal-warnings",
  "-Xlint"
)

val projectSettings = Seq(
  name := "grpc-reactive",
  description := "",
  version := "1.0",
  scalaVersion := "2.12.6",
  organization := "Sebastian Bach",
  scalacOptions ++= additionalScalacOptions
)

val dependencies = Seq(
  "io.monix" %% "monix" % "3.0.0-RC1",
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

lazy val root = (project in file("."))
  .settings(projectSettings: _*)
  .settings(libraryDependencies ++= dependencies)
