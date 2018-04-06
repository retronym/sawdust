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
val jpmsPatchTest = settingKey[Boolean]("Test sources should be patched into the compile module")

jpmsPatchTest in Global := false

autoScalaLibrary in ThisBuild := false
crossPaths in ThisBuild := false

fork in run in ThisBuild := true

fork in test in ThisBuild := true

def commonConfigSettings: Seq[Setting[_]] = Seq(
  javaSource := (javaSource.value / jpmsModuleName.value),
  javacOptions := overwriteModulePath(dependencyClasspath.value.map(_.data))(javacOptions.value),
  javaOptions := overwriteModulePath(dependencyClasspath.value.map(_.data) ++ (if (configuration.value != Test || !jpmsPatchTest.value) List(classDirectory.value) else Nil) )(javaOptions.value),
  // apparently this needs to follow `--patch-module`: https://gist.github.com/d696b44b817e6fd0b2e457ad805e62cb
  javaOptions := {
    val modAndMainClass = jpmsModuleName.value + "/" + mainClass.value.getOrElse("")
    overwriteOption("--module", modAndMainClass, moveToEnd = true)(javaOptions.value)
  }
)

def patchModuleSettings: Seq[Setting[_]] = Seq(
  javacOptions in Test ++= (if (jpmsPatchTest.value) List(
    "--patch-module", jpmsModuleName.value + "=" + (javaSource in Test).value
  ) else Nil),
  javaOptions in Test ++= (if (jpmsPatchTest.value) List(
    "--patch-module", jpmsModuleName.value + "=" + (classDirectory in Test).value
  ) else Nil)
)

def commonSettings: Seq[Setting[_]] = (
  patchModuleSettings ++ Seq(Compile, Test).flatMap(scope => inConfig(scope)(commonConfigSettings))
)

val sawdustAlpha = project.in(file("modules/sawdust.alpha")).settings(
  jpmsModuleName := "sawdust.alpha",
  jpmsPatchTest := true,
  libraryDependencies ++= Seq(slf4jDep, apiguardianDep, junitJupiterApiDep, junitJupiterEngineDep, junitJupiterConsoleDep),
  javacOptions in Test ++= List("--add-modules", "org.junit.jupiter.api", "--add-reads", jpmsModuleName.value + "=" + "org.junit.jupiter.api"),
  javaOptions in Test ++= List("--add-modules", "ALL-MODULE-PATH", "--add-reads", jpmsModuleName.value + "=" + "org.junit.jupiter.api")
).settings(commonSettings)

val userView = project.in(file("modules/user.view")).dependsOn(sawdustAlpha).settings(
  jpmsModuleName := "user.view",
  libraryDependencies ++= Seq(slf4jDep, apiguardianDep, junitJupiterApiDep, junitJupiterEngineDep, junitJupiterConsoleDep)
).settings(commonSettings)

def overwriteModulePath(modulePath: Seq[File])(options: Seq[String]): Seq[String] = {
  val modPathString = modulePath.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
  overwriteOption("--module-path", modPathString)(options)
}
def overwriteOption(option: String, value: String, moveToEnd: Boolean = false)(options: Seq[String]): Seq[String] = {
  val index = options.indexWhere(_ == option)
  if (index == -1) options ++ List(option, value)
  else if (moveToEnd) options.patch(index, Nil, 2) ++ List(option, value)
  else options.patch(index + 1, List(value), 1)
}
