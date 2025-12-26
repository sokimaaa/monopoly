ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"

lazy val commonSettings = Seq(
  idePackagePrefix := Some("com.sokima.monopoly")
)

lazy val mnplDomain = (project in file("mnpl-domain"))
  .settings(commonSettings)
  .settings(
    name := "mnpl-domain"
  )

lazy val mnplApplication = (project in file("mnpl-application"))
  .settings(commonSettings)
  .settings(
    name := "mnpl-application"
  )
  .dependsOn(mnplDomain)

lazy val mnplInfraCli = (project in file("mnpl-infra-cli"))
  .settings(commonSettings)
  .settings(
    name := "mnpl-infra-cli"
  )
  .dependsOn(mnplApplication, mnplDomain)

lazy val mnplInfraPersistence = (project in file("mnpl-infra-persistence"))
  .settings(commonSettings)
  .settings(
    name := "mnpl-infra-persistence"
  )
  .dependsOn(mnplApplication, mnplDomain)

lazy val mnplInfraEvent = (project in file("mnpl-infra-event"))
  .settings(commonSettings)
  .settings(
    name := "mnpl-infra-event"
  )
  .dependsOn(mnplApplication, mnplDomain)

lazy val mnplInfraFactory = (project in file("mnpl-infra-factory"))
  .settings(commonSettings)
  .settings(
    name := "mnpl-infra-factory"
  )
  .dependsOn(mnplApplication, mnplDomain)

lazy val mnplBootstrap = (project in file("mnpl-bootstrap"))
  .settings(commonSettings)
  .settings(
    name := "mnpl-bootstrap"
  )
  .dependsOn(
    mnplApplication,
    mnplDomain,
    mnplInfraCli,
    mnplInfraPersistence,
    mnplInfraEvent,
    mnplInfraFactory
  )

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "monopoly"
  )
  .aggregate(
    mnplDomain,
    mnplApplication,
    mnplInfraCli,
    mnplInfraPersistence,
    mnplInfraEvent,
    mnplInfraFactory,
    mnplBootstrap
  )
