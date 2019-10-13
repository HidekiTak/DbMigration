package jp.hotbrain.db.migration

import java.net.InetAddress
import java.sql.{Connection, SQLException}

import scala.collection.mutable.ArrayBuffer

private[migration] object MigrationSchema {

  final val hostName: String = {
    InetAddress.getLocalHost.getHostName
  }

  def process(fileName: String, con: Connection, schema: String, sqls: Seq[(String, Seq[String])], dryRun: Boolean = false, semaphorePrefix: String = "migration"): Unit = {
    println(s"start: $fileName")
    println(s"schema: $schema")
    withSemaphore(
      con,
      schema,
      semaphorePrefix,
      checkDone(_, schema, sqls.sortBy(_._1), semaphorePrefix).foreach(processOne(con, semaphorePrefix, _, dryRun)))
  }

  private[this] def checkDone(con: Connection, schema: String, sqls: Seq[(String, Seq[String])], semaphorePrefix: String): Seq[(String, Seq[String])] = {
    val done = getDone(con, schema, semaphorePrefix)
    if (done.isEmpty) {
      sqls
    } else {
      sqls.filterNot(tuple => done.contains(tuple._1))
    }
  }

  private[this] def getDone(con: Connection, schema: String, semaphorePrefix: String): Array[String] = {
    con.setCatalog(schema)
    val stmt = con.createStatement()
    try {
      val rs = stmt.executeQuery(s"SELECT `name` FROM `${semaphorePrefix}_jobs`")
      try {
        if (!rs.next()) {
          Array.empty[String]
        } else {
          val result = ArrayBuffer[String]()
          do {
            result.append(rs.getString(1))
          } while (rs.next())
          result.toArray
        }
      } finally {
        rs.close()
      }
    } finally {
      stmt.close()
    }
  }


  private[this] def processOne(con: Connection, semaphorePrefix: String, tuple: (String, Seq[String]), dryRun: Boolean): Unit = {
    if (dryRun) {
      println(s"${con.getCatalog}.${tuple._1}: start")
      tuple._2.foreach { sql =>
        println(sql)
        println
      }
      println(s"${con.getCatalog}.${tuple._1}: done")
    } else {
      val now = System.currentTimeMillis()
      con.setAutoCommit(false)
      val stmt = con.createStatement()
      try {
        tuple._2.foreach(stmt.addBatch)
        stmt.executeBatch()
        val prep = con.prepareStatement(s"INSERT INTO `${semaphorePrefix}_jobs`(`name`,`at`)VALUES(?,?)")
        try {
          prep.setString(1, tuple._1)
          prep.setLong(2, now)
          prep.executeUpdate()
        } finally {
          prep.close()
        }
        con.commit()
        println(s"${con.getCatalog}.${tuple._1}: done")
      } catch {
        case ex: Throwable =>
          System.err.println(s"${con.getCatalog}.${tuple._1}: ${ex.getMessage}")
          con.rollback()
          throw ex
      } finally {
        stmt.close()
      }
      con.setAutoCommit(true)
    }
  }

  private[this] def createSchema(con: Connection, schema: String): Unit = {
    val stmt = con.createStatement()
    try {
      stmt.execute(s"CREATE SCHEMA `$schema`")
      println(s"$schema: created")
    } catch {
      case ex: SQLException if 0 <= ex.getMessage.indexOf("Can't create database") =>
      case ex: Throwable =>
        System.err.println(s"$schema: fail to create")
        ex.printStackTrace(System.err)
        throw ex
    } finally {
      stmt.close()
    }
  }

  private[this] def withSemaphore(con: Connection, schema: String, semaphorePrefix: String, callback: Connection => Unit): Unit = {
    try {
      con.setCatalog(schema)
      withSemaphoreSub(con, schema, semaphorePrefix, callback)
    } catch {
      case ex: SQLException if 0 <= ex.getMessage.indexOf("Unknown database") =>
        createSchema(con, schema)
        createSemaphore(con, schema, semaphorePrefix)
        withSemaphoreSub(con, schema, semaphorePrefix, callback)
      case ex: SQLException if 0 <= ex.getMessage.indexOf("doesn't exist") =>
        createSemaphore(con, schema, semaphorePrefix)
        withSemaphoreSub(con, schema, semaphorePrefix, callback)
    }
  }

  private[this] final def sqlGetSemaphore(semaphorePrefix: String): String =
    s"""INSERT IGNORE INTO `${semaphorePrefix}_semaphore`
(`id`,`executor`,`start_at`)
VALUES(1,?,?)
ON DUPLICATE KEY UPDATE
`executor`=IF(`start_at`+60000>VALUES(`start_at`),`executor`,VALUES(`executor`)),
`start_at`=IF(`start_at`+60000>VALUES(`start_at`),`start_at`,VALUES(`start_at`))"""

  private[this] def withSemaphoreSub(con: Connection, schema: String, semaphorePrefix: String, callback: Connection => Unit): Unit = {
    con.setCatalog(schema)
    val now = System.currentTimeMillis
    val execName = hostName + "_" + Thread.currentThread().getId
    val prep = con.prepareStatement(sqlGetSemaphore(semaphorePrefix))

    try {
      prep.setString(1, execName)
      prep.setLong(2, now)
      if (prep.executeUpdate() <= 0) {
        // Semaphoreが取れなかった
        throw new Exception(s"fail to get a semaphore $schema")
      }
    } finally {
      prep.close()
    }
    try {
      callback(con)
    } finally {
      val prep = con.prepareStatement(s"DELETE FROM `${semaphorePrefix}_semaphore` WHERE `id`=1 AND `executor`=? AND `start_at`=?")
      try {
        prep.setString(1, execName)
        prep.setLong(2, now)
        prep.executeUpdate()
      } finally {
        prep.close()
      }
    }
  }

  private[this] def sqlCreateJob(semaphorePrefix: String): String =
    s"""CREATE TABLE `${semaphorePrefix}_jobs` (
  `name` VARCHAR(255) NOT NULL,
  `at` BIGINT NOT NULL,
  PRIMARY KEY (`name`))"""

  private[this] def sqlCreateSemaphore(semaphorePrefix: String): String =
    s"""CREATE TABLE `${semaphorePrefix}_semaphore` (
  `id` INT NOT NULL,
  `executor` VARCHAR(255) NOT NULL,
  `start_at` BIGINT NOT NULL,
  PRIMARY KEY (`id`));
"""

  private[this] def createSemaphore(con: Connection, schema: String, semaphorePrefix: String): Unit = {
    con.setCatalog(schema)
    con.setAutoCommit(false)
    val stmt = con.createStatement()
    try {
      stmt.addBatch(sqlCreateJob(semaphorePrefix))
      stmt.addBatch(sqlCreateSemaphore(semaphorePrefix))
      stmt.executeBatch()
      con.commit()
    } catch {
      case ex: Throwable =>
        con.rollback()
        throw ex
    } finally {
      stmt.close()
    }
  }
}
