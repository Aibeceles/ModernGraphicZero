ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "2.12.18"

lazy val sparkVersion = "3.5.1"

lazy val root = (project in file("."))
  .settings(
    name := "polys",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
      "org.neo4j" % "neo4j-jdbc-driver" % "4.0.0",
      "org.scalatest" %% "scalatest" % "3.2.18" % Test
    ),
    Test / fork := true
  )
