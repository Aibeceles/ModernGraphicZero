"""Write node and edge DataFrames to Neo4j via the Spark Connector."""

from pyspark.sql import DataFrame


def _neo4j_base_options(writer, url: str, user: str, password: str, database: str):
    """Apply shared Neo4j connection options to a DataFrameWriter."""
    return (
        writer
        .format("org.neo4j.spark.DataSource")
        .option("url", url)
        .option("authentication.basic.username", user)
        .option("authentication.basic.password", password)
        .option("database", database)
    )


def write_nodes(
    nodes_df: DataFrame,
    url: str,
    user: str,
    password: str,
    database: str,
) -> None:
    """Write the vertices DataFrame to Neo4j as Dnode nodes.

    Uses MERGE semantics keyed on ``vmResult`` so that duplicate writes
    produce upserts rather than duplicate nodes.

    Args:
        nodes_df: Vertices DataFrame with columns id, n, d, z, muList,
            rootList, wNum.
        url: Neo4j Bolt URL.
        user: Neo4j username.
        password: Neo4j password.
        database: Target Neo4j database name.
    """
    writer = (
        _neo4j_base_options(
            nodes_df.coalesce(1).write.mode("Append"),
            url, user, password, database,
        )
        .option("batch.size", 5000)
        .option(
            "query",
            "MERGE (d:Dnode {vmResult: event.id}) "
            "ON CREATE SET d.n = event.n, d.d = event.d, d.z = event.z, "
            "d.muList = event.muList, d.rootList = event.rootList, "
            "d.wNum = event.wNum, d.pArray = event.pArray",
        )
    )
    writer.save()


def write_edges(
    edges_df: DataFrame,
    url: str,
    user: str,
    password: str,
    database: str,
) -> None:
    """Write the edges DataFrame to Neo4j as zMap relationships.

    Uses key-based strategy so that source and target nodes are matched
    by their ``vmResult`` property.

    Args:
        edges_df: Edges DataFrame with columns src, dst, relationship.
        url: Neo4j Bolt URL.
        user: Neo4j username.
        password: Neo4j password.
        database: Target Neo4j database name.
    """
    writer = (
        _neo4j_base_options(
            edges_df.coalesce(1).write.mode("Append"),
            url, user, password, database,
        )
        .option(
            "query",
            "MATCH (src:Dnode {vmResult: event.src}) "
            "MATCH (dst:Dnode {vmResult: event.dst}) "
            "MERGE (src)-[:zMap]->(dst)",
        )
    )
    writer.save()
