package io.github.retronym.sbtjpms

import sbt.{AutoPlugin, Compile, Def, ModuleID, Setting, Test, inConfig, settingKey, _}
import sbt.Keys._

object JpmsPlugin extends AutoPlugin{
  object autoImport {
    val jpmsModuleName = settingKey[String]("JPMS module name")
    val jpmsLibraryDependencyName = settingKey[List[(ModuleID, String)]]("JPMS module name of dependencies")
    val jpmsPatchTest = settingKey[Boolean]("Test sources should be patched into the compile module")
  }

  import autoImport._

  override def trigger = super.trigger

  override lazy val projectSettings: Seq[Setting[_]] = commonSettings

  def commonConfigSettings: Seq[Setting[_]] = Seq(
    javaSource := (javaSource.value / jpmsModuleName.value),
    javacOptions := overwriteModulePath(dependencyClasspath.value.map(_.data))(javacOptions.value),
    javaOptions := overwriteModulePath(dependencyClasspath.value.map(_.data) ++ (if (configuration.value != Test || !jpmsPatchTest.??(false).value) List(classDirectory.value) else Nil) )(javaOptions.value),
    // apparently this needs to follow `--patch-module`: https://gist.github.com/d696b44b817e6fd0b2e457ad805e62cb
    javaOptions := {
      val modAndMainClass = jpmsModuleName.value + "/" + mainClass.value.getOrElse("")
      overwriteOption("--module", modAndMainClass, moveToEnd = true)(javaOptions.value)
    }
  )

  def patchModuleSettings: Seq[Setting[_]] = Seq(
    javacOptions in Test ++= (if (jpmsPatchTest.??(false).value) List(
      "--patch-module", jpmsModuleName.value + "=" + (javaSource in Test).value
    ) else Nil),
    javaOptions in Test ++= (if (jpmsPatchTest.??(false).value) List(
      "--patch-module", jpmsModuleName.value + "=" + (classDirectory in Test).value
    ) else Nil)
  )

  private def testAddModulesAddReads(isJavaOptions: Boolean): Def.Initialize[Task[Seq[String]]] = Def.task  {
    val compileDeps: Seq[Attributed[File]] = (dependencyClasspath in Compile).value
    val compileFileSet = compileDeps.iterator.map(_.data)
    val testDeps = (dependencyClasspath in Test).value
    val testOnlyDeps = testDeps.filterNot(dep => compileFileSet.contains(dep.data))
    println(testOnlyDeps)
    val map: Map[(String, String), String] = jpmsLibraryDependencyName.value.iterator.map(it => ((it._1.organization, it._1.name), it._2)).toMap
    val testOnlyJpmsNames: Seq[String] = testOnlyDeps.flatMap(dep => dep.get(moduleID.key).flatMap((m: ModuleID) => map.get((m.organization, m.name))))
    println(jpmsLibraryDependencyName.value)
    println("testOnlyJpmsNames = " + testOnlyJpmsNames)
    if (testOnlyJpmsNames.isEmpty) Nil
    else {
      val jpmsModulePath = testOnlyJpmsNames.mkString(java.io.File.pathSeparator)
      val sameModuleNameAsCompile = (jpmsModuleName in Test).value == (jpmsModuleName in Compile).value
      val addModules = List("--add-modules", if (isJavaOptions) "ALL-MODULE-PATH" else testOnlyJpmsNames.mkString(","))
      if (!sameModuleNameAsCompile) addModules
      else {
        val addReads = testOnlyJpmsNames.distinct.flatMap(name => List("--add-reads", jpmsModuleName.value + "=" + name))
        addModules ++ addReads
      }
    }
  }

  def testModuleSettings: Seq[Setting[_]] = Seq(
    javacOptions in Test ++= testAddModulesAddReads(isJavaOptions = false).value,
    javaOptions in Test ++= testAddModulesAddReads(isJavaOptions = true).value
  ) ++ patchModuleSettings

  def commonSettings: Seq[Setting[_]] = (
    testModuleSettings ++ Seq(Compile, Test).flatMap(scope => inConfig(scope)(commonConfigSettings))
  )

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
}

