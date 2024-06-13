// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import scala.sys.process.Process

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(common: _*)
  .settings(
    name := "asset-changes-main",
    InputKey[Unit]("verifyResourceContains") := {
      val args                         = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path :: status :: assertions = args
      ScriptedTools.verifyResourceContains(path, status.toInt, assertions)
    },
    InputKey[Unit]("checkUnzipListing") := {
      val args = Def.spaceDelimited("<zipfile> <difffile>").parsed
      val baseDir = (ThisBuild / baseDirectory).value

      if (args.length != 2) {
        sys.error("Usage: checkUnzipListing <zipfile> <difffile>")
      } else {
        val zipfile = args(0)
        val vanilla_difffile = args(1)

        val unzipcmd = s"unzip -l $zipfile" // We assume the system has unzip installed...
        val unzipOutput = Process(unzipcmd, baseDir).!!

        val difffile = if(vanilla_difffile.endsWith(".jar.txt")) {
          vanilla_difffile
        } else {
          vanilla_difffile + (if(scalaBinaryVersion.value == "3") {
            ".scala3.jar.txt"
          } else {
            ".scala2.jar.txt"
          })
        }

        val difffile_content = IO.readLines(new File(difffile)).mkString("\n") + "\n"

        println(s"\nComparing unzip listing of file $zipfile with contents of $difffile")
        println(s"### $zipfile")
        print(unzipOutput)
        println(s"### $difffile")
        print(difffile_content)
        println(s"###")

        if (unzipOutput != difffile_content) {
          sys.error(s"Unzip listing ('$unzipcmd') does not match expected content!")
        } else {
          println(s"Listing of $zipfile as expected.")
        }
        println()
      }
    },
  )
  .dependsOn(subproj)
  .aggregate(subproj)

lazy val subproj = (project in file("subproj"))
  .enablePlugins(PlayScala)
  .settings(common: _*)
  .settings(
    name := "asset-changes-sub",
  )

def common: Seq[Setting[?]] = Seq(
  version := "1.0-SNAPSHOT",
  PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
  scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
  updateOptions := updateOptions.value.withLatestSnapshots(false),
  update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
  libraryDependencies += guice,
)
