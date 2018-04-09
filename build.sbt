import _root_.io.github.retronym.sbtjpms._

name := "sawdust"

lazy val root = project.in(file(".")).aggregate(sawdustAlpha, userView)

organization in ThisBuild := "de.sormuras"

version in ThisBuild := "1.0-SNAPSHOT"

val slf4jDep = "org.slf4j" % "slf4j-api" % System.getProperty("slf4j.version", "1.8.0-beta1")
val apiguardianDep = "org.apiguardian" % "apiguardian-api" % System.getProperty("apiguardian.version", "1.0.0") % Test
val jupiterVersion = System.getProperty("jupiter.version", "5.1.0")
val junitJupiterApiDep = "org.junit.jupiter" % "junit-jupiter-api" % jupiterVersion % Test
val junitJupiterEngineDep = "org.junit.jupiter" % "junit-jupiter-engine" % jupiterVersion % Test
val junitJupiterConsoleDep = "org.junit.platform" % "junit-platform-console" % System.getProperty("platform.version", "1.1.0") % Test

jpmsLibraryDependencyName in ThisBuild := {
  List(junitJupiterApiDep -> "org.junit.jupiter.api")
}

autoScalaLibrary in ThisBuild := false
crossPaths in ThisBuild := false

fork in run in ThisBuild := true

fork in test in ThisBuild := true

val sawdustAlpha = project.in(file("modules/sawdust.alpha")).enablePlugins(JpmsPlugin).settings(
  jpmsModuleName := "sawdust.alpha",
  jpmsPatchTest := true,
  libraryDependencies ++= Seq(slf4jDep, apiguardianDep, junitJupiterApiDep, junitJupiterEngineDep, junitJupiterConsoleDep)
)

val userView = project.in(file("modules/user.view")).enablePlugins(JpmsPlugin).dependsOn(sawdustAlpha).settings(
  jpmsModuleName := "user.view",
  libraryDependencies ++= Seq(slf4jDep, apiguardianDep, junitJupiterApiDep, junitJupiterEngineDep, junitJupiterConsoleDep)
)
