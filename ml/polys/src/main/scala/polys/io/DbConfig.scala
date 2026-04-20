package polys.io

import java.io.File
import java.io.FileInputStream
import java.util.Properties

object DbConfig {
  /** Preferred location when CWD is repository root (Aibeceles). */
  private val PathFromRepoRoot = "ml/polys/db.properties"

  /** Preferred location when CWD is the polys module directory. */
  private val PathFromPolysDir = "db.properties"

  private lazy val props: Properties = {
    val p = new Properties()
    val resolved = resolvePropertiesFile()
    val fis = new FileInputStream(resolved)
    try p.load(fis)
    finally fis.close()
    p
  }

  /** Resolves db.properties: JVM override, then repo-relative, then module-local, then walk parents. */
  private def resolvePropertiesFile(): File = {
    sys.props.get("polys.db.properties") match {
      case Some(overridePath) =>
        val f = new File(overridePath)
        require(f.isFile, s"polys.db.properties not found or not a file: $overridePath")
        f
      case None =>
        val candidates =
          Seq(new File(PathFromRepoRoot), new File(PathFromPolysDir)) ++
            walkParentsForMlPolysDb(new File(System.getProperty("user.dir"))).toSeq

        candidates.find(_.isFile).getOrElse {
          throw new IllegalStateException(
            s"Neo4j db.properties not found. Tried: $PathFromRepoRoot, $PathFromPolysDir, " +
              "and ml/polys/db.properties under parent directories of user.dir. " +
              "Set -Dpolys.db.properties=<absolute path> or copy db.properties.example to ml/polys/db.properties."
          )
        }
    }
  }

  /** From startDir, walk up and return first existing .../ml/polys/db.properties */
  private def walkParentsForMlPolysDb(startDir: File): Option[File] = {
    var dir = startDir.getCanonicalFile
    var depth = 0
    while (dir != null && depth < 12) {
      val candidate = new File(dir, PathFromRepoRoot)
      if (candidate.isFile) return Some(candidate)
      dir = dir.getParentFile
      depth += 1
    }
    None
  }

  def get(key: String): String = {
    val value = props.getProperty(key)
    require(value != null && value.nonEmpty, s"Missing required property: $key")
    value
  }

  def protocol: String = s"${get("neo4j.url")}?database=${get("neo4j.database")}"
}
