package polys.config

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

final case class AppConfig(
    pipelineName: String,
    inputPath: String,
    inputFormat: String,
    artifactsRoot: String,
    runId: String,
    rangeLow: String,
    rangeHigh: String,
    maxN: String,
    dimension: String
)

object AppConfig {
  private val RunIdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

  def fromArgs(args: Array[String]): AppConfig = {
    val kv = args
      .filter(_.contains("="))
      .map { arg =>
        val pair = arg.split("=", 2)
        pair(0).stripPrefix("--") -> pair(1)
      }
      .toMap

    val runId = kv.getOrElse(
      "runId",
      LocalDateTime.now(ZoneOffset.UTC).format(RunIdFormatter)
    )

    AppConfig(
      pipelineName = kv.getOrElse("pipelineName", "strongestPath"),
      inputPath = kv.getOrElse("inputPath", ""),
      inputFormat = kv.getOrElse("inputFormat", "parquet"),
      artifactsRoot = kv.getOrElse("artifactsRoot", "ml/polys/artifacts"),
      runId = runId,
      rangeLow = kv.getOrElse("rangeLow", "2"),
      rangeHigh = kv.getOrElse("rangeHigh", "7"),
      maxN = kv.getOrElse("maxN", "8"),
      dimension = kv.getOrElse("dimension", "2")
    )
  }
}
