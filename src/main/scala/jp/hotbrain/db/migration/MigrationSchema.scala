package jp.hotbrain.db.migration

import java.net.InetAddress
import java.sql.{Connection, SQLException}

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

trait QuerySet {
  def GetSemaphore(semaphorePrefix: String): String

  def CheckSemaphore(semaphorePrefix: String): String

  def SemaphoreDone(semaphorePrefix: String): String

  def JobDone(semaphorePrefix: String): String

  def DoneList(semaphorePrefix: String): String

  def JobCreate(semaphorePrefix: String): String

  def SemaphoreCreate(semaphorePrefix: String): String

  def SchemaCreate(con: Connection, schema: String): Unit
}

object QuerySet {
  def apply(con: Connection): QuerySet = {
    con.getMetaData.getDatabaseProductName match {
      case "PostgreSQL" =>
        postgres.QuerySet
      case _ =>
        mysql.QuerySet
    }
  }
}

private[migration] object MigrationSchema {

  final val hostName: String = InetAddress.getLocalHost.getHostName

  def job(jobName: String, con: Connection, schema: String, callback: Connection => Unit, semaphorePrefix: String = "migration"): Unit = {
    val querySet: QuerySet = QuerySet(con)
    withSemaphore(
      con, querySet, schema, semaphorePrefix,
      checkDone(_, querySet, schema, Seq((jobName, Seq[String]())), semaphorePrefix)
        .map(_ => callback(con))
        .foreach(_ => jobDone(con, querySet, jobName, semaphorePrefix)))
  }

  private[this] def jobDone(con: Connection, querySet: QuerySet, jobName: String, semaphorePrefix: String): Unit = {
    val prep = con.prepareStatement(querySet.JobDone(semaphorePrefix))
    try {
      prep.setString(1, jobName)
      prep.setLong(2, System.currentTimeMillis())
      prep.executeUpdate()
    } finally {
      prep.close()
    }
  }

  def process(fileName: String, con: Connection, schema: String, sqls: Seq[(String, Seq[String])], dryRun: Boolean = false, semaphorePrefix: String = "migration"): Unit = {
    val querySet: QuerySet = QuerySet(con)
    println(s"[setup] $nowString: DbMigration: start: $fileName for $schema")
    var again = 10
    while (0 < again) {
      println(s"[setup] $nowString: DbMigration: $again")
      try {
        withSemaphore(
          con, querySet, schema, semaphorePrefix,
          checkDone(_, querySet, schema, sqls.sortBy(_._1), semaphorePrefix).foreach(processOne(con, querySet, semaphorePrefix, _, dryRun)))
        again = -1
      } catch {
        case _: ExSemaphoreNotGet =>
          again = again - 1
          Thread.sleep(100)
      }
    }
  }

  private[this] def checkDone(con: Connection, querySet: QuerySet, schema: String, sqls: Seq[(String, Seq[String])], semaphorePrefix: String): Seq[(String, Seq[String])] = {
    val done = getDone(con, querySet, schema, semaphorePrefix)
    if (done.isEmpty) {
      sqls
    } else {
      sqls.filterNot(tuple => done.contains(tuple._1))
    }
  }

  private[this] def getDone(con: Connection, querySet: QuerySet, schema: String, semaphorePrefix: String): Array[String] = {
    this.setCatalog(con, querySet, schema)
    val stmt = con.createStatement()
    try {
      val rs = stmt.executeQuery(querySet.DoneList(semaphorePrefix))
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

  private[this] def processOne(con: Connection, querySet: QuerySet, semaphorePrefix: String, tuple: (String, Seq[String]), dryRun: Boolean): Unit = {
    println(s"[setup] $nowString: DbMigration: ${con.getCatalog}.${tuple._1}: start")
    if (dryRun) {
      tuple._2.foreach { sql =>
        println(sql)
        println
      }
      println(s"[setup] $nowString: DbMigration: ${con.getCatalog}.${tuple._1}: done")
    } else {
      var last_sql: String = ""
      con.setAutoCommit(false)
      val stmt = con.createStatement()
      try {
        tuple._2.zipWithIndex.foreach { s =>
          if (verbose) {
            println(s"[setup] $nowString: DbMigration: ${con.getCatalog}.${tuple._1}(${s._2}): '${s._1.replaceAllLiterally("\n", "\\n")}'")
          }
          last_sql = s._1
          stmt.execute(s._1)
        }
        jobDone(con, querySet, tuple._1, semaphorePrefix)
        con.commit()
        println(s"[setup] $nowString: DbMigration: ${con.getCatalog}.${tuple._1}: done")
      } catch {
        case ex: Throwable =>
          System.err.println(s"[sever] $nowString: DbMigration: ${con.getCatalog}.${tuple._1}: '${ex.getMessage}'\n$last_sql")
          con.rollback()
          throw ex
      } finally {
        stmt.close()
      }
      con.setAutoCommit(true)
    }
  }
  //
  //  private[this] def createSchema(con: Connection, schema: String): Unit = {
  //    val stmt = con.createStatement()
  //    try {
  //      stmt.execute(s"CREATE SCHEMA `$schema`")
  //      println(s"[setup] $nowString: DbMigration: $schema: created")
  //    } catch {
  //      case ex: SQLException if 0 <= ex.getMessage.indexOf("Can't create database") =>
  //      case ex: Throwable =>
  //        System.err.println(s"[sever] $nowString: DbMigration: $schema: fail to create")
  //        ex.printStackTrace(System.err)
  //        throw ex
  //    } finally {
  //      stmt.close()
  //    }
  //  }

  private[this] def setCatalog(con: Connection, querySet: QuerySet, schema: String): Unit = {
    try {
      con.setCatalog(schema)
    } catch {
      case ex: SQLException if ex.getMessage.startsWith("Unknown database") =>
        querySet.SchemaCreate(con, schema)
        con.setCatalog(schema)
    }
  }

  private[this] def withSemaphore(con: Connection, querySet: QuerySet, schema: String, semaphorePrefix: String, callback: Connection => Unit): Unit = {
    try {
      println(s"withSemaphore: con.setCatalog($schema)")
      this.setCatalog(con, querySet, schema)
      println(s"con.setCatalog($schema)")
      withSemaphoreSub(con, querySet, schema, semaphorePrefix, callback)
    } catch {
      case ex: SQLException if ex.getMessage.contains("doesn't exist") || ex.getMessage.contains("does not exist") =>
        println(ex.getMessage)
        if (ex.getMessage.contains("migration_semaphore")) {
          createSemaphore(con, querySet, schema, semaphorePrefix)
        }
        withSemaphoreSub(con, querySet, schema, semaphorePrefix, callback)
      case ex: Throwable =>
        println(ex.getMessage)
        ex.printStackTrace()
        throw ex
    }
  }

  class ExSemaphoreNotGet(mess: String) extends Exception(mess)

  private[this] def withSemaphoreSub(con: Connection, querySet: QuerySet, schema: String, semaphorePrefix: String, callback: Connection => Unit): Unit = {
    val now = System.currentTimeMillis
    val execName = hostName + "_" + Thread.currentThread.threadId
    val prep = con.prepareStatement(querySet.GetSemaphore(semaphorePrefix))
    try {
      prep.setString(1, execName)
      prep.setLong(2, now)
      prep.executeUpdate()
    } finally {
      prep.close()
    }

    val prep2 = con.prepareStatement(querySet.CheckSemaphore(semaphorePrefix))
    try {
      val rs = prep2.executeQuery()
      try {
        if (!rs.next() || rs.getString("executor") != execName) {
          // Semaphoreが取れなかった
          throw new ExSemaphoreNotGet(s"""fail to get a semaphore("$schema"): "${rs.getString("executor")}" != "$execName"""")
        }
      } finally {
        rs.close()
      }
    } finally {
      prep2.close()
    }

    try {
      callback(con)
    } finally {
      val prep = con.prepareStatement(querySet.SemaphoreDone(semaphorePrefix))
      try {
        prep.setString(1, execName)
        prep.setLong(2, now)
        prep.executeUpdate()
      } finally {
        prep.close()
      }
    }
  }

  private[this] def createSemaphore(con: Connection, querySet: QuerySet, schema: String, semaphorePrefix: String): Unit = {
    con.setAutoCommit(true)
    val stmt = con.createStatement()
    try {
      stmt.addBatch(querySet.JobCreate(semaphorePrefix))
      stmt.addBatch(querySet.SemaphoreCreate(semaphorePrefix))
      stmt.executeBatch()
    } finally {
      stmt.close()
    }
  }
}
