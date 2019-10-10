package jp.hotbrain.db.migration

import java.sql.DriverManager

import org.junit.{Before, Test}

class MigratorConfigTest {

  private[this] final val connectionString = System.getenv("DB_MIG_CON_STR")

  @Before
  def beforeTest(): Unit = {
    // drop schemas
    val con = DriverManager.getConnection(connectionString)
    try {
      val stmt = con.createStatement()
      try {
        Seq("local_tenant_db_migration", "tenant_00001_db_migration", "tenant_00002_db_migration", "tenant_00003_db_migration")
          .foreach(schema => stmt.addBatch(s"DROP SCHEMA IF EXISTS `$schema`"))
      } finally {
        stmt.close()
      }
    } finally {
      con.close()
    }
  }

  @Test
  def parseTest(): Unit = {
    val configs = MigratorConfig(FileSystem(FileSystem.prefixJar + "/jp/hotbrain/db/migration/filesystemtest1"))
    configs.foreach { conf =>
      conf.iterator.foreach(_.exec { (con, schemaName) =>
        MigrationSchema.process(
          con,
          schemaName,
          conf.sqls
        )
      })
    }

    val configs2 = MigratorConfig(FileSystem(FileSystem.prefixJar + "/jp/hotbrain/db/migration/filesystemtest2"))
    configs2.foreach { conf =>
      conf.iterator.foreach(_.exec { (con, schemaName) =>
        MigrationSchema.process(con, schemaName, conf.sqls)
      })
    }

  }
}
