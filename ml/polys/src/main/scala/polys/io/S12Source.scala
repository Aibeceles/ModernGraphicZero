package polys.io

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types.{StringType, StructField, StructType}

object S12Source {
  val CanonicalSchema: StructType = StructType(
    Seq(
      StructField("index", StringType, nullable = false),
      StructField("maxN", StringType, nullable = false),
      StructField("rowScalar", StringType, nullable = false),
      StructField("divisor", StringType, nullable = false),
      StructField("scalar", StringType, nullable = false),
      StructField("degree", StringType, nullable = false)
    )
  )

  def load(
      spark: SparkSession,
      inputPath: String,
      inputFormat: String,
      rangeLow: String,
      rangeHigh: String,
      maxN: String,
      dimension: String
  ): DataFrame = {
    val reader = spark.read
    val df = inputFormat.toLowerCase match {
      case "neo4j" =>
        Neo4jS12Source.load(
          spark = spark,
          rangeLow = rangeLow,
          rangeHigh = rangeHigh,
          maxN = maxN,
          dimension = dimension
        )
      case "parquet" =>
        require(inputPath.nonEmpty, "inputPath is required for file-based extractS12 stage")
        reader.parquet(inputPath)
      case "csv" =>
        require(inputPath.nonEmpty, "inputPath is required for file-based extractS12 stage")
        reader.option("header", "true").csv(inputPath)
      case "json" =>
        require(inputPath.nonEmpty, "inputPath is required for file-based extractS12 stage")
        reader.json(inputPath)
      case other => throw new IllegalArgumentException(s"Unsupported inputFormat: $other")
    }
    CanonicalSchema.fieldNames.foldLeft(df) { (acc, col) =>
      if (acc.columns.contains(col)) acc.withColumn(col, acc(col).cast(StringType))
      else acc.withColumn(col, org.apache.spark.sql.functions.lit(null).cast(StringType))
    }.select(CanonicalSchema.fieldNames.map(org.apache.spark.sql.functions.col): _*)
  }
}
