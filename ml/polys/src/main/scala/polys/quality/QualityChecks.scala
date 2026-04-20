package polys.quality

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

object QualityChecks {
  def requireColumns(df: DataFrame, required: Seq[String], label: String): Unit = {
    val missing = required.filterNot(df.columns.contains)
    require(missing.isEmpty, s"$label missing required columns: ${missing.mkString(", ")}")
  }

  def requireNoNulls(df: DataFrame, columns: Seq[String], label: String): Unit = {
    val nullExpr = columns.map(c => sum(when(col(c).isNull, 1).otherwise(0)).alias(c))
    val row = df.agg(nullExpr.head, nullExpr.tail: _*).first()
    val offenders = columns.filter(c => row.getAs[Long](c) > 0)
    require(offenders.isEmpty, s"$label has nulls in: ${offenders.mkString(", ")}")
  }
}
