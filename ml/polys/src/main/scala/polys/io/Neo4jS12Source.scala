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
  private val Query =
    """CYPHER runtime=interpreted
      |UNWIND range(toInteger(?),toInteger(?)) AS n
      |WITH toString(n) AS iN, ? as iM
      |MATCH (v:VertexNode)<-[vI:VertexIndexedBy]-(i:IndexedBy {N:iN,MaxN:iM,Dimension:?})-[]->(tS:TwoSeqFactor)
      |RETURN iN as idx, iM as mxN, vI.twoSeq as rScalar, vI.divisor as div, v.Scalar as scl, v.Degree as deg
      |ORDER BY idx, deg
      |""".stripMargin

  def load(
      spark: SparkSession,
      rangeLow: String,
      rangeHigh: String,
      maxN: String,
      dimension: String
  ): DataFrame = {
    Class.forName("org.neo4j.jdbc.Driver").newInstance()
    val conn = DriverManager.getConnection(
      DbConfig.protocol,
      DbConfig.get("neo4j.user"),
      DbConfig.get("neo4j.password")
    )
    conn.setAutoCommit(false)
    try runQuery(spark, conn, rangeLow, rangeHigh, maxN, dimension)
    finally conn.close()
  }

  private def runQuery(
      spark: SparkSession,
      conn: Connection,
      rangeLow: String,
      rangeHigh: String,
      maxN: String,
      dimension: String
  ): DataFrame = {
    val ps = conn.prepareStatement(Query)
    try {
      ps.setString(1, rangeLow)
      ps.setString(2, rangeHigh)
      ps.setString(3, maxN)
      ps.setString(4, dimension)

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
