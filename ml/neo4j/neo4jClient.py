"""
Neo4j Client - A simple database connector for Neo4j.

This module provides a minimal, single-responsibility client for connecting to Neo4j
databases, executing Cypher queries, and returning results as pandas DataFrames.
"""

from neo4j import GraphDatabase, exceptions
import pandas as pd


class Neo4jClient:
    """
    A simple Neo4j client that runs Cypher queries and returns results as pandas DataFrame,
    with error handling for connection and query execution.
    
    This client follows the single responsibility principle, focusing solely on:
    - Database connection management
    - Query execution
    - Result conversion to pandas DataFrames
    
    Example:
        >>> client = Neo4jClient("bolt://localhost:7687", "neo4j", "password")
        >>> df = client.run_query("MATCH (n) RETURN n LIMIT 10", "neo4j")
        >>> print(df)
        >>> client.close()
    """
    
    def __init__(self, uri: str, user: str, password: str):
        """
        Initialize the Neo4j client and establish a connection.
        
        Args:
            uri (str): Neo4j connection URI (e.g., 'bolt://localhost:7687')
            user (str): Database username
            password (str): Database password
            
        Raises:
            ConnectionError: If connection to Neo4j fails
            
        Example:
            >>> client = Neo4jClient("bolt://localhost:7687", "neo4j", "password")
        """
        try:
            self.driver = GraphDatabase.driver(uri, auth=(user, password))
        except exceptions.Neo4jError as err:
            raise ConnectionError(f"Failed to create Neo4j driver: {err}")

    def run_query(self, query: str, database: str, parameters: dict = None) -> pd.DataFrame:
        """
        Execute a Cypher query and return results as a pandas DataFrame.
        
        If no records are returned (e.g., for CREATE/DELETE queries), returns a DataFrame
        with execution summary information including nodes created, relationships created,
        properties set, and execution timing.
        
        Args:
            query (str): Cypher query string to execute
            database (str): Target database name
            parameters (dict, optional): Query parameters for parameterized queries. Defaults to None.
            
        Returns:
            pd.DataFrame: Query results or execution summary
            
        Raises:
            RuntimeError: If query execution fails
            
        Examples:
            >>> # Simple query
            >>> df = client.run_query("MATCH (n) RETURN n LIMIT 5", "neo4j")
            
            >>> # Parameterized query
            >>> query = "MATCH (p:Person {name: $name}) RETURN p"
            >>> df = client.run_query(query, "neo4j", {"name": "Alice"})
            
            >>> # Create query (returns summary)
            >>> query = "CREATE (n:Person {name: $name})"
            >>> df = client.run_query(query, "neo4j", {"name": "Bob"})
        """
        try:
            with self.driver.session(database=database) as session:
                result = session.run(query, parameters or {})
                records = [record.data() for record in result]
                summary = result.consume()
        except exceptions.Neo4jError as err:
            raise RuntimeError(f"Error executing Cypher query: {err}")

        if not records:
            # Populate DataFrame with summary information
            summary_info = {
                "nodes_created": summary.counters.nodes_created,
                "relationships_created": summary.counters.relationships_created,
                "properties_set": summary.counters.properties_set,
                "result_available_after": summary.result_available_after,
                "result_consumed_after": summary.result_consumed_after
            }
            df = pd.DataFrame([summary_info])
        else:
            # Build DataFrame from records
            df = pd.DataFrame(records)

        return df

    def close(self):
        """
        Close the Neo4j driver connection.
        
        Should be called when done using the client to properly release resources.
        Consider using the client in a try-finally block or context manager pattern
        to ensure proper cleanup.
        
        Example:
            >>> client = Neo4jClient(uri, user, password)
            >>> try:
            ...     df = client.run_query(query, database)
            ... finally:
            ...     client.close()
        """
        try:
            self.driver.close()
        except exceptions.Neo4jError as err:
            print(f"Warning: Error closing Neo4j driver: {err}")
