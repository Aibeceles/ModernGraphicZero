package polys.app

import org.apache.spark.sql.SparkSession

import polys.config.AppConfig
import polys.io.{ArtifactWriter, S12Source}
import polys.quality.QualityChecks
import polys.transform.StrongestPathTransforms

object MainJob {
  def main(args: Array[String]): Unit = {
    val config = AppConfig.fromArgs(args)

    val spark = SparkSession
      .builder()
      .appName("polys-strongest-path")
      .getOrCreate()

    try {
      val extract = S12Source.load(
        spark = spark,
        inputPath = config.inputPath,
        inputFormat = config.inputFormat,
        rangeLow = config.rangeLow,
        rangeHigh = config.rangeHigh,
        maxN = config.maxN,
        dimension = config.dimension
      )
      QualityChecks.requireColumns(extract, Seq("index", "maxN", "rowScalar", "divisor", "scalar", "degree"), "extractS12")
      QualityChecks.requireNoNulls(extract, Seq("index", "maxN", "rowScalar", "divisor", "scalar", "degree"), "extractS12")

      val grouped = StrongestPathTransforms.groupedDegreeSum(extract)
      val rowPivot = StrongestPathTransforms.rowScalarDegreePivot(extract)
      val aggPivot = StrongestPathTransforms.aggregatedScalarDegreePivot(grouped)
      val reduced = StrongestPathTransforms.reducedResult(grouped)

      ArtifactWriter.writeStageParquet(extract, config.artifactsRoot, config.pipelineName, config.runId, "extractS12")
      ArtifactWriter.writeStageParquet(grouped, config.artifactsRoot, config.pipelineName, config.runId, "groupedDegreeSum")
      ArtifactWriter.writeStageParquet(rowPivot, config.artifactsRoot, config.pipelineName, config.runId, "rowScalarDegreePivot")
      ArtifactWriter.writeStageParquet(aggPivot, config.artifactsRoot, config.pipelineName, config.runId, "aggregatedScalarDegreePivot")
      ArtifactWriter.writeStageParquet(reduced, config.artifactsRoot, config.pipelineName, config.runId, "reducedResult")

      val metricJson =
        s"""{"runId":"${config.runId}","pipelineName":"${config.pipelineName}","stages":{"extractS12":${extract.count()},"groupedDegreeSum":${grouped.count()},"rowScalarDegreePivot":${rowPivot.count()},"aggregatedScalarDegreePivot":${aggPivot.count()},"reducedResult":${reduced.count()}}}"""

      ArtifactWriter.writeRunMetricJson(metricJson, config.artifactsRoot, config.pipelineName, config.runId)
      ArtifactWriter.updateLatestPointer(config.artifactsRoot, config.pipelineName, config.runId)
    } finally {
      spark.stop()
    }
  }
}
