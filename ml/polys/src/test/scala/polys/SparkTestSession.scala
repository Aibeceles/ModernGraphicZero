package polys

import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, Suite}

trait SparkTestSession extends BeforeAndAfterAll { self: Suite =>
  @transient protected var spark: SparkSession = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    spark = SparkSession.builder().master("local[2]").appName("polys-test").getOrCreate()
  }

  override protected def afterAll(): Unit = {
    if (spark != null) spark.stop()
    super.afterAll()
  }
}
