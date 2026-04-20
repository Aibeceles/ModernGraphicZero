package polys.transform

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.DecimalType

object StrongestPathTransforms {
  private val NumType = DecimalType(38, 10)

  def groupedDegreeSum(extractS12: DataFrame): DataFrame = {
    extractS12
      .withColumn("index_num", col("index").cast(DecimalType(25, 0)))
      .withColumn("maxN_num", col("maxN").cast(DecimalType(25, 0)))
      .withColumn("rowScalar_num", col("rowScalar").cast(NumType))
      .withColumn("divisor_num", col("divisor").cast(NumType))
      .withColumn("scalar_num", col("scalar").cast(NumType))
      .withColumn("degree_num", col("degree").cast(DecimalType(25, 0)))
      .withColumn(
        "weighted_scalar",
        when(col("divisor_num") === lit(0), lit(null).cast(NumType))
          .otherwise((col("scalar_num") * col("rowScalar_num")) / col("divisor_num"))
      )
      .groupBy(col("maxN_num").alias("maxN"), col("index_num").alias("index"), col("degree_num").alias("degree"))
      .agg(sum("weighted_scalar").alias("scalar_result"))
      .orderBy(col("index").asc, col("degree").asc)
  }

  def rowScalarDegreePivot(extractS12: DataFrame): DataFrame = {
    extractS12
      .withColumn("N", col("index").cast(DecimalType(25, 0)))
      .withColumn("rowScalar_num", col("rowScalar").cast(DecimalType(25, 0)))
      .withColumn("degree_num", col("degree").cast(DecimalType(25, 0)))
      .withColumn("scalar_num", col("scalar").cast(NumType))
      .groupBy("N", "rowScalar_num")
      .pivot("degree_num")
      .agg(sum("scalar_num"))
      .orderBy(col("N").asc, col("rowScalar_num").asc)
  }

  def aggregatedScalarDegreePivot(grouped: DataFrame): DataFrame = {
    grouped
      .withColumn("N", col("index").cast(DecimalType(25, 0)))
      .withColumn("degree_num", col("degree").cast(DecimalType(25, 0)))
      .withColumn("scalar_num", col("scalar_result").cast(NumType))
      .groupBy("N")
      .pivot("degree_num")
      .agg(sum("scalar_num"))
      .orderBy(col("N").asc)
  }

  def reducedResult(grouped: DataFrame): DataFrame = {
    grouped
      .withColumn("maxN_num", col("maxN").cast(DecimalType(25, 0)))
      .withColumn("index_num", col("index").cast(DecimalType(25, 0)))
      .withColumn("degree_int", col("degree").cast("int"))
      .withColumn("scalar_num", col("scalar_result").cast(NumType))
      .withColumn("power_term", pow(col("maxN_num"), col("degree_int")))
      .withColumn("eval_term", col("scalar_num") * col("power_term"))
      .groupBy(col("maxN_num").alias("maxN"), col("index_num").alias("index"))
      .agg(sum("eval_term").alias("index_result"))
      .orderBy(col("index").asc)
  }
}
