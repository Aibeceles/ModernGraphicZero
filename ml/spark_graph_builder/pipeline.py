"""Main pipeline worker: Parquet -> DataFrames -> GraphFrame -> Neo4j.

Each worker process creates one SparkSession, then repeatedly claims one
Parquet file from the staging directory via an atomic lock-file mechanism
(see ``file_claim``), processes it, and archives it to the ``done/``
subfolder. Multiple workers may run concurrently against the same staging
directory; each will claim a distinct file.
"""

from graphframes import GraphFrame
from pyspark.sql.functions import col

from spark_graph_builder.config import (
    NEO4J_DATABASE,
    NEO4J_PASSWORD,
    NEO4J_URL,
    NEO4J_USER,
    PARQUET_BATCH_DIR,
    create_spark_session,
)
from spark_graph_builder.edges import build_edges
from spark_graph_builder.file_claim import archive_and_release, claim_next_parquet, release_claim
from spark_graph_builder.nodes import build_vertices, read_raw_batches
from spark_graph_builder.writer import write_edges, write_nodes


def run_pipeline(
    spark,
    batch_dir: str = PARQUET_BATCH_DIR,
    persist_to_neo4j: bool = True,
) -> GraphFrame | None:
    """Execute the graph-build pipeline for one claimed Parquet file.

    Atomically claims the oldest unclaimed Parquet file in *batch_dir*, builds
    deduplicated vertices and chain-derived edges, constructs an in-memory
    GraphFrame, and optionally persists the graph to Neo4j.  On success the 
    Parquet file is archived to ``<batch_dir>/done/``.  On any exception the
    lock is released so another worker can retry the file.

    Args:
        batch_dir: Path to the directory containing ``batches_*.parquet``
            files written by the Java ``writeBatchToParquet()`` method.
        persist_to_neo4j: When ``True``, write the final graph to Neo4j via
            the Spark Connector.  _Set to ``False`` for local analysis only.

    Returns:
        The constructed GraphFrame, or ``None`` if no unclaimed file was found.
    """
    parquet_path = claim_next_parquet(batch_dir)
    if parquet_path is None:
        return None

    print(f"Claimed: {parquet_path}")

    try:
        raw_df = read_raw_batches(spark, parquet_path)
        raw_df.cache()

        raw_df.printSchema()
        print("--- Sample raw rows ---")
        raw_df.select("vmResult", "rootList", "wNum", "pArray").show(50, truncate=False)

        print("--- chain_position vs wNum alignment ---")
        raw_df.select("file_batch", "batch_id", "chain_position", "wNum", "vmResult") \
              .orderBy("file_batch", "batch_id", "chain_position") \
              .show(50, truncate=False)

        vertices_df = build_vertices(raw_df)
        edges_df = build_edges(raw_df)

        print("--- Vertices sample ---")
        vertices_df.select("id", "rootList", "wNum", "pArray").show(50, truncate=False)

        raw_count = raw_df.count()
        non_empty_raw = raw_df.filter(col("rootList") != "[]").count()
        non_empty_vtx = vertices_df.filter(col("rootList") != "[]").count()
        print(f"Raw rows: {raw_count}")
        print(f"Non-empty rootList in raw: {non_empty_raw}")
        print(f"Non-empty rootList in vertices: {non_empty_vtx}")

        graph = GraphFrame(vertices_df, edges_df)

        print(f"Vertices: {vertices_df.count()}")
        print(f"Edges:    {edges_df.count()}")

        if persist_to_neo4j:
            write_nodes(vertices_df, NEO4J_URL, NEO4J_USER, NEO4J_PASSWORD, NEO4J_DATABASE)
            write_edges(edges_df, NEO4J_URL, NEO4J_USER, NEO4J_PASSWORD, NEO4J_DATABASE)
            print("Graph persisted to Neo4j.")

        raw_df.unpersist()

    except Exception:
        release_claim(parquet_path)
        raise

    archive_and_release(parquet_path, batch_dir)
    print(f"Archived: {parquet_path}")
    return graph


def run_worker(
    batch_dir: str = PARQUET_BATCH_DIR,
    persist_to_neo4j: bool = True,
) -> int:
    """Create one Spark session and drain the staging directory."""
    spark = create_spark_session()
    processed_count = 0

    try:
        while True:
            graph = run_pipeline(
                spark,
                batch_dir=batch_dir,
                persist_to_neo4j=persist_to_neo4j,
            )
            if graph is None:
                break
            processed_count += 1
    finally:
        spark.stop()

    print(f"Worker complete. FilesProcessed={processed_count}")
    return processed_count


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="Build Dnode graph from one Parquet file and persist to Neo4j.",
    )
    parser.add_argument(
        "--batch-dir",
        default=PARQUET_BATCH_DIR,
        help="Path to the Parquet staging directory (default: %(default)s)",
    )
    parser.add_argument(
        "--no-neo4j",
        action="store_true",
        help="Skip writing to Neo4j; build in-memory GraphFrame only.",
    )
    args = parser.parse_args()
 
    run_worker(
        batch_dir=args.batch_dir,
        persist_to_neo4j=not args.no_neo4j,
    )

    #spark = create_spark_session()
    #try:
    #    run_pipeline(
    #        spark,
    #        batch_dir=args.batch_dir,
    #        persist_to_neo4j=not args.no_neo4j,
    #    )
    #finally:
    #    spark.stop()    
