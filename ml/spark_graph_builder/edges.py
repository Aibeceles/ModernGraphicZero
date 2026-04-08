"""Edge DataFrame construction from chain structure."""

from pyspark.sql import DataFrame, Window
from pyspark.sql.functions import col, lead, lit


def build_edges(raw_df: DataFrame) -> DataFrame:
    """Derive zMap edges from consecutive chain positions.

    Within each (file_batch, batch_id) group, rows are ordered by
    chain_position.  Each row is linked to the next row in the chain
    via a directed ``zMap`` edge: source -> target.

    The last element in each chain has no successor, so the ``lead``
    window function returns NULL for ``dst``; those rows are filtered out.

    Args:
        raw_df: Raw DataFrame read from Parquet staging directory.
            Must contain columns: vmResult, file_batch, batch_id,
            chain_position.

    Returns:
        Edges DataFrame with columns: src, dst, relationship.
        GraphFrames expects ``src`` and ``dst`` columns on the edges
        DataFrame.
    """

# original chain window when batch is syncronized with chain length

    chain_window = (
        Window
        .partitionBy("file_batch", "batch_id")
        .orderBy("chain_position")
    )

#    chain_window = (
#        Window
#        .partitionBy("pArray")
#        .orderBy("wNum")
#    )

    return (
        raw_df
        .select(
            col("vmResult").alias("src"),
            lead("vmResult").over(chain_window).alias("dst"),
            lit("zMap").alias("relationship"),
        )
        .filter(col("dst").isNotNull())
        .dropDuplicates(["src", "dst"])
    )
