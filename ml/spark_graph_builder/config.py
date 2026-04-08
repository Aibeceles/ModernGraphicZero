"""Configuration for the Spark GraphFrames pipeline."""

import os
from dotenv import load_dotenv

load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))

PARQUET_BATCH_DIR = os.environ.get(
    "PARQUET_BATCH_DIR",
    r"C:\Users\tomas\JavaProjects\Aibeceles\ml\data\parquet_batches",
)

NEO4J_URL = os.environ.get("NEO4J_URL", "bolt://localhost:7687")
NEO4J_USER = os.environ.get("NEO4J_USER", "neo4j")
NEO4J_PASSWORD = os.environ.get("NEO4J_PASSWORD", "ChiefQuippy")
NEO4J_DATABASE = os.environ.get("NEO4J_DATABASE", "tagtest")

SPARK_PACKAGES = ",".join([
    "org.neo4j:neo4j-connector-apache-spark_2.12:5.3.8_for_spark_3",
    "graphframes:graphframes:0.8.4-spark3.5-s_2.12",
])

SPARK_JDK_HOME = os.environ.get(
    "SPARK_JAVA_HOME",
    r"C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot",
)
HADOOP_HOME = os.environ.get("HADOOP_HOME", r"C:\hadoop")


def create_spark_session(app_name="DnodeGraphBuilder"):
    """Build a local-mode SparkSession with Neo4j + GraphFrames packages."""
    os.environ["JAVA_HOME"] = SPARK_JDK_HOME 
    os.environ["HADOOP_HOME"] = HADOOP_HOME
    os.environ["PATH"] = (
        os.path.join(HADOOP_HOME, "bin") + os.pathsep + os.environ.get("PATH", "")
    )

    from pyspark.sql import SparkSession

    return (
        SparkSession.builder
        .appName(app_name)
        .master("local[1]")
        .config("spark.jars.packages", SPARK_PACKAGES)
        # Windows: force driver to bind on loopback so BlockManager can
        # resolve its own host and register a non-null executorId.
        # Without this, local-mode Spark on Windows spins indefinitely
        # with NullPointerException in BlockManagerMasterEndpoint.
        .config("spark.driver.host", "127.0.0.1")
        .config("spark.driver.bindAddress", "127.0.0.1")
        .getOrCreate()
    )
