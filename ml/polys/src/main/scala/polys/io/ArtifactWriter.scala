package polys.io

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.apache.spark.sql.DataFrame

object ArtifactWriter {
  private def parquetBase(artifactsRoot: String, pipelineName: String): String =
    s"$artifactsRoot/parquet/$pipelineName"

  def writeStageParquet(
      df: DataFrame,
      artifactsRoot: String,
      pipelineName: String,
      runId: String,
      stageName: String
  ): String = {
    val path = s"${parquetBase(artifactsRoot, pipelineName)}/$runId/$stageName"
    df.write.mode("overwrite").parquet(path)
    path
  }

  def writeRunMetricJson(
      json: String,
      artifactsRoot: String,
      pipelineName: String,
      runId: String
  ): String = {
    val metricsDir = Paths.get(s"$artifactsRoot/metrics/$pipelineName/$runId")
    Files.createDirectories(metricsDir)
    val outFile = metricsDir.resolve("run_metrics.json")
    Files.write(outFile, json.getBytes(StandardCharsets.UTF_8))
    outFile.toString
  }

  def updateLatestPointer(
      artifactsRoot: String,
      pipelineName: String,
      runId: String
  ): String = {
    val pointerFile = Paths.get(s"${parquetBase(artifactsRoot, pipelineName)}/latest.txt")
    Files.createDirectories(pointerFile.getParent)
    Files.write(pointerFile, runId.getBytes(StandardCharsets.UTF_8))
    pointerFile.toString
  }
}
