package polys

import org.apache.spark.sql.Row
import org.scalatest.funsuite.AnyFunSuite

import polys.transform.StrongestPathTransforms

class StrongestPathTransformsSpec extends AnyFunSuite with SparkTestSession {
  import spark.implicits._

  test("groupedDegreeSum aggregates weighted scalar by maxN/index/degree") {
    val input = Seq(
      ("7", "8", "1", "2", "6", "0"),
      ("7", "8", "2", "2", "6", "0")
    ).toDF("index", "maxN", "rowScalar", "divisor", "scalar", "degree")

    val out = StrongestPathTransforms.groupedDegreeSum(input)
    val row = out.select("scalar_result").first()
    assert(row.getDecimal(0).toPlainString.startsWith("9"))
  }

  test("reducedResult matches known notebook style result 128 for index 8 polynomial") {
    val grouped = Seq(
      ("8", "8", "0", "1416"),
      ("8", "8", "1", "-417"),
      ("8", "8", "2", "32")
    ).toDF("maxN", "index", "degree", "scalar_result")

    val out = StrongestPathTransforms.reducedResult(grouped)
    val result = out.select("index_result").collect().toSeq.map((r: Row) => r.getDouble(0)).head
    assert(math.abs(result - 128.0d) < 1e-9)
  }
}
