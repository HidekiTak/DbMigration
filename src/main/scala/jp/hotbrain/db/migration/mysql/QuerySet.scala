package jp.hotbrain.db.migration.mysql

import java.sql.{Connection, SQLException}

import jp.hotbrain.db.migration
import jp.hotbrain.db.migration.nowString

object QuerySet extends migration.QuerySet {

  override def GetSemaphore(semaphorePrefix: String): String = {
    s"""INSERT INTO `${semaphorePrefix}_semaphore`
(`id`,`executor`,`start_at`)
VALUES(1,?,?)
ON DUPLICATE KEY UPDATE
`executor`=IF(`start_at`+60000>VALUES(`start_at`),`executor`,VALUES(`executor`)),
`start_at`=IF(`executor`=VALUES(`executor`),`start_at`,VALUES(`start_at`))"""
  }

  override def CheckSemaphore(semaphorePrefix: String): String = {
    s"SELECT * FROM `${semaphorePrefix}_semaphore` WHERE `id`=1"
  }

  def SemaphoreDone(semaphorePrefix: String): String = {
    s"DELETE FROM `${semaphorePrefix}_semaphore` WHERE `id`=1 AND `executor`=? AND `start_at`=?"
  }

  override def JobDone(semaphorePrefix: String): String = {
    s"INSERT INTO `${semaphorePrefix}_jobs`(`name`,`at`)VALUES(?,?)"
  }

  override def DoneList(semaphorePrefix: String): String = {
    s"SELECT `name` FROM `${semaphorePrefix}_jobs`"
  }


  def JobCreate(semaphorePrefix: String): String = {
    s"""CREATE TABLE `${semaphorePrefix}_jobs` (
  `name` VARCHAR(255) NOT NULL,
  `at` BIGINT NOT NULL,
  PRIMARY KEY (`name`))"""
  }

  def SemaphoreCreate(semaphorePrefix: String): String = {
    s"""CREATE TABLE `${semaphorePrefix}_semaphore` (
  `id` INT NOT NULL,
  `executor` VARCHAR(255) NOT NULL,
  `start_at` BIGINT NOT NULL,
  PRIMARY KEY (`id`));
"""
  }

  override def SchemaCreate(con: Connection, schema: String): Unit = {
    val stmt = con.createStatement()
    try {
      stmt.execute(s"CREATE DATABASE `$schema`")
      println(s"[setup] $nowString: DbMigration: $schema: created")
    } catch {
      case ex: SQLException if 0 <= ex.getMessage.indexOf("Can't create database") =>
      case ex: Throwable =>
        System.err.println(s"[sever] $nowString: DbMigration: $schema: fail to create")
        ex.printStackTrace(System.err)
        throw ex
    } finally {
      stmt.close()
    }
  }
}
