lazy val root = project.in(file(".")).aggregate(sawdustAlpha, userView)

organization in ThisBuild := "de.sormuras"

version in ThisBuild := "1.0-SNAPSHOT"

val slf4jDep = "org.slf4j" % "slf4j-api" % System.getProperty("slf4j.version", "1.8.0-beta1")
val apiguardianDep = "org.apiguardian" % "apiguardian-api" % System.getProperty("apiguardian.version", "1.0.0") % Test
val jupiterVersion = System.getProperty("jupiter.version", "5.1.0")
val junitJupiterApiDep = "org.junit.jupiter" % "junit-jupiter-api" % jupiterVersion % Test
val junitJupiterEngineDep = "org.junit.jupiter" % "junit-jupiter-engine" % jupiterVersion % Test
val junitJupiterConsoleDep = "org.junit.platform" % "junit-platform-console" % System.getProperty("platform.version", "1.1.0") % Test

val jpmsModuleName = settingKey[String]("JPMS module name")


val commonConfigSettings = Seq(
  javaSource := (javaSource.value / jpmsModuleName.value),
  javacOptions ++= List("--module-path", fullClasspath.value.mkString(java.io.File.pathSeparator))
)

val commonSettings: Seq[Setting[_]] = Seq(Compile, Test).flatMap(scope => inConfig(scope)(commonConfigSettings))

val sawdustAlpha = project.in(file("modules/sawdust.alpha")).settings(
  jpmsModuleName := "sawdust.alpha",
  libraryDependencies ++= Seq(slf4jDep, apiguardianDep, junitJupiterApiDep, junitJupiterEngineDep, junitJupiterConsoleDep)
).settings(commonSettings)

val userView = project.in(file("modules/user.view")).dependsOn(sawdustAlpha).settings(
  jpmsModuleName := "user.view"
).settings(commonSettings : _*)
