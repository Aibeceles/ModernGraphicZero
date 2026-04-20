package polys

import org.scalatest.funsuite.AnyFunSuite

import polys.io.{Neo4jS12Row, Neo4jS12Source, S12Source}

class Neo4jS12SourceSpec extends AnyFunSuite with SparkTestSession {
  test("Neo4j row mapping preserves canonical column order") {
    val rows = Seq(
      Neo4jS12Row(
        index = "7",
        maxN = "8",
        rowScalar = "32",
        divisor = "2",
        scalar = "56",
        degree = "0"
      )
    )

    val df = Neo4jS12Source.toDataFrame(spark, rows)
    assert(df.columns.toSeq == S12Source.CanonicalSchema.fieldNames.toSeq)
  }

  test("Neo4j row mapping values are compatible with strongest-path transforms") {
    val rows = Seq(
      Neo4jS12Row("7", "8", "1", "2", "16", "0"),
      Neo4jS12Row("7", "8", "1", "2", "-2", "1"),
      Neo4jS12Row("7", "8", "1", "2", "1", "2")
    )
    val df = Neo4jS12Source.toDataFrame(spark, rows)
    val first = df.first()
    assert(first.getAs[String]("index") == "7")
    assert(first.getAs[String]("maxN") == "8")
    assert(first.getAs[String]("degree") == "0")
  }
}
