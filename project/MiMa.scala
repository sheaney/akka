/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka

import sbt._
import sbt.Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._

object MiMa extends AutoPlugin {

  private val latestMinorOf25 = 3
  private val latestMinorOf24 = 18

  override def requires = MimaPlugin
  override def trigger = allRequirements

  override val projectSettings = Seq(
    mimaPreviousArtifacts := akkaPreviousArtifacts(name.value, organization.value, scalaBinaryVersion.value)
  )

  def akkaPreviousArtifacts(projectName: String, organization: String, scalaBinaryVersion: String): Set[sbt.ModuleID] = {
    val versions: Seq[String] = {
      val akka24NoStreamVersions = Seq("2.4.0", "2.4.1")
      val akka25Versions = (0 to latestMinorOf25).map(patch => s"2.5.$patch")
      val akka24StreamVersions = (2 to 12) map ("2.4." + _)
      val akka24WithScala212 =
        (13 to latestMinorOf24)
          .map ("2.4." + _)
          .filterNot(_ == "2.4.15") // 2.4.15 was released from the wrong branch and never announced

      val akka242NewArtifacts = Seq(
        "akka-stream",
        "akka-stream-testkit"
      )
      val akka250NewArtifacts = Seq(
        "akka-persistence-query"
      )

      scalaBinaryVersion match {
        case "2.11" =>
          if (akka250NewArtifacts.contains(projectName)) akka25Versions
          else {
            if (!akka242NewArtifacts.contains(projectName)) akka24NoStreamVersions
            else Seq.empty
          } ++ akka24StreamVersions ++ akka24WithScala212 ++ akka25Versions

        case "2.12" =>
          akka24WithScala212 ++ akka25Versions
      }
    }

    val akka25PromotedArtifacts = Set(
      "akka-distributed-data"
    )

    // check against all binary compatible artifacts
    versions.map { v =>
      val adjustedProjectName =
        if (akka25PromotedArtifacts(projectName) && v.startsWith("2.4"))
          projectName + "-experimental"
        else
          projectName
      organization %% adjustedProjectName % v
    }.toSet
  }
}
