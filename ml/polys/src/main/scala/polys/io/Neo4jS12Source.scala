package polys.io

import java.sql.{Connection, DriverManager}

import scala.collection.mutable.ArrayBuffer

import org.apache.spark.sql.{DataFrame, SparkSession}

final case class Neo4jS12Row(
    index: String,
    maxN: String,
    rowScalar: String,
    divisor: String,
    scalar: String,
    degree: String
)

object Neo4jS12Source {
  // Quadratic (Dimension="2") extract produced by TwoPolynomialGenerator.jar (twoPoly sink).
  // Reads rowScalar from the TwoSeqFactor node (sink writes twoSeq there, not on the rel)
  // and uses a constant divisor of "1" because the twoPoly sink does not store a divisor.
  // Binds: 1=rangeLow, 2=rangeHigh, 3=maxN. Dimension is hardcoded to "2".
  private val QueryDim2 =
    """CYPHER runtime=interpreted
      |UNWIND range(toInteger(?),toInteger(?)) AS n
      |WITH toString(n) AS iN, ? AS iM
      |MATCH (v:VertexNode)<-[:VertexIndexedBy]-(i:IndexedBy {N: iN, MaxN: iM, Dimension: "2"})-[:TwoFactor]->(tS:TwoSeqFactor)
      |RETURN iN AS idx, iM AS mxN, tS.twoSeq AS rScalar, "1" AS div, v.Scalar AS scl, v.Degree AS deg
      |ORDER BY idx, rScalar, deg
      |""".stripMargin

  def load(
      spark: SparkSession,
      rangeLow: String,
      rangeHigh: String,
      maxN: String,
      dimension: String
  ): DataFrame = {
    require(
      dimension == "2",
      s"Neo4jS12Source currently supports only dimension=2 (quadratics from twoPoly lineage); got: $dimension"
    )
    Class.forName("org.neo4j.jdbc.Driver").newInstance()
    val conn = DriverManager.getConnection(
      DbConfig.protocol,
      DbConfig.get("neo4j.user"),
      DbConfig.get("neo4j.password")
    )
    conn.setAutoCommit(false)
    try runQuery(spark, conn, rangeLow, rangeHigh, maxN)
    finally conn.close()
  }

  private def runQuery(
      spark: SparkSession,
      conn: Connection,
      rangeLow: String,
      rangeHigh: String,
      maxN: String
  ): DataFrame = {
    val ps = conn.prepareStatement(QueryDim2)
    try {
      ps.setString(1, rangeLow)
      ps.setString(2, rangeHigh)
      ps.setString(3, maxN)

      val rs = ps.executeQuery()
      val rows = new ArrayBuffer[Neo4jS12Row]()
      while (rs.next()) {
        rows += Neo4jS12Row(
          index = rs.getString("idx"),
          maxN = rs.getString("mxN"),
          rowScalar = rs.getString("rScalar"),
          divisor = rs.getString("div"),
          scalar = rs.getString("scl"),
          degree = rs.getString("deg")
        )
      }
      conn.commit()
      toDataFrame(spark, rows.toSeq)
    } finally {
      ps.close()
    }
  }

  def toDataFrame(spark: SparkSession, rows: Seq[Neo4jS12Row]): DataFrame = {
    import spark.implicits._
    rows.toDS().toDF(
      "index",
      "maxN",
      "rowScalar",
      "divisor",
      "scalar",
      "degree"
    )
  }
}
