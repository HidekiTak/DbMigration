package jp.hotbrain.db.migration

import java.sql.{Connection, DriverManager}

import org.junit.Assert._
import org.junit.Test

import scala.collection.mutable.ListBuffer

class MigrationSchemaTest {
  private[this] final val connectionString = System.getenv("DB_MIG_CON_STR")

  private[this] def withTable(catalogName: String, callback: (Connection, String) => Unit): Unit = {
    val con = DriverManager.getConnection(connectionString)
    try {
      callback(con, catalogName)
    } finally {
      val stmt = con.createStatement()
      try {
        stmt.execute(s"DROP DATABASE `$catalogName`")
      } finally {
        stmt.close()
      }
      con.close()
    }
  }

  private[this] def getInitOneName(con: Connection): Seq[String] = {
    val stmt = con.createStatement()
    try {
      val rs = stmt.executeQuery("SELECT * FROM `init_one`")
      try {
        if (!rs.next()) {
          Nil
        } else {
          val result = ListBuffer[String]()
          do {
            result.append(rs.getString("name"))
          } while (rs.next())
          result.sorted.toSeq
        }
      } finally {
        rs.close()
      }
    } finally {
      stmt.close()
    }
  }

  @Test
  def xTest(): Unit = {
    withTable(
      s"migration_test_${MigrationSchema.hostName}_${System.currentTimeMillis()}",
      (con, catalogName) => {
        MigrationSchema.process(
          "xTest",
          con,
          catalogName,
          Seq(
            ("00001_init",
              Seq(
                """CREATE TABLE `init_one`(
  `name` VARCHAR(255) NOT NULL,
  `at` BIGINT NOT NULL,
  PRIMARY KEY (`name`))"""))
          )
        )
        assertEquals(Seq(), getInitOneName(con))

        MigrationSchema.process(
          "xTest",
          con,
          catalogName,
          Seq(
            ("00001_init",
              Seq(
                """CREATE TABLE `init_one`(
  `name` VARCHAR(255) NOT NULL,
  `at` BIGINT NOT NULL,
  PRIMARY KEY (`name`))""")),
            ("00002_insert_data",
              Seq(s"INSERT INTO `init_one`(`name`,`at`)VALUES('migration_test',${System.currentTimeMillis()})"))
          )
        )
        assertEquals(Seq("migration_test"), getInitOneName(con))
      }
    )
  }
}
