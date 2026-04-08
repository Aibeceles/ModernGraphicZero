"""Node DataFrame construction and deduplication."""

from pyspark.sql import DataFrame, SparkSession
from pyspark.sql import Window
from pyspark.sql.functions import col, first, length, row_number


def read_raw_batches(spark: SparkSession, parquet_path: str) -> DataFrame:
    """Read a single Parquet batch file into a DataFrame.

    Args:
        spark: Active SparkSession.
        parquet_path: Path to a single ``batches_*.parquet`` file claimed
            exclusively by this pipeline invocation.

    Returns:
        DataFrame with columns: vmResult, n, d, z, muList, rootList, wNum,
        batch_id, chain_position, file_batch.
    """
    return spark.read.parquet(parquet_path)


def build_vertices(raw_df: DataFrame) -> DataFrame:
    """Deduplicate raw batch rows by vmResult to produce the vertices DataFrame.

    GraphFrames requires a column named ``id`` on the vertices DataFrame.
    The first occurrence (by lowest file_batch, then batch_id) wins for
    property values when duplicates exist.

    Args:
        raw_df: Raw DataFrame read from Parquet staging directory.

    Returns:
        Vertices DataFrame with columns: id, n, d, z, muList, rootList, wNum,
        pArray.
    """
    parray_df = (
        raw_df
        .filter(col("wNum") == 0)
        .groupBy(col("vmResult").alias("_vmr"))
        .agg(first("pArray").alias("pArray"))
    )

    w = Window.partitionBy("id").orderBy(length("rootList").desc())

    return (
        raw_df
        .select(
            col("vmResult").alias("id"),
            "n", "d", "z", "muList", "rootList", "wNum",
        )
        .withColumn("_rn", row_number().over(w))
        .filter(col("_rn") == 1)
        .drop("_rn")
        .join(parray_df, col("id") == col("_vmr"), "left")
        .drop("_vmr")
    )
