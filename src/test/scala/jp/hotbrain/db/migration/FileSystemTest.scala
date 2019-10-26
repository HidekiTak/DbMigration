package jp.hotbrain.db.migration

import org.junit.Test
import org.junit.Assert._

class FileSystemTest {

  private[this] final val expected: String =
    """directory: filesystemtest1
  directory: common
    file: 00000.conf
single:"jp.hotbrain.db.migration.RuleFactorySingleForTest"
schemaName: "${ENV}_tenant_db_migration"
    file: 00001_setup.sql
CREATE TABLE `test_1` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `meta` TEXT NOT NULL,
  PRIMARY KEY (`id`));

CREATE TABLE `test_2` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `meta` TEXT NOT NULL,
  PRIMARY KEY (`id`));
    file: 00002_change.sql
ALTER TABLE `test_1`
ADD COLUMN `ver` BIGINT NOT NULL AFTER `meta`;
  /directory: common
  directory: tenant
    file: 00000.conf
multi: "jp.hotbrain.db.migration.RuleFactoryMultiForTestInit"
    file: 00001_setup.sql
CREATE TABLE `test_5` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `meta` TEXT NOT NULL,
  PRIMARY KEY (`id`));

CREATE TABLE `test_6` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `meta` TEXT NOT NULL,
  PRIMARY KEY (`id`));
    file: 00002_change.sql
ALTER TABLE `test_5`
ADD COLUMN `ver` BIGINT NOT NULL AFTER `meta`;
  /directory: tenant
/directory: filesystemtest1
"""

  @Test
  def osTest(): Unit = {
    val ofOs = FileSystem(getClass, "file:" + ProjectRoot.of("test_data/jp/hotbrain/db/migration/filesystemtest1").toAbsolutePath.toString)
    assertEquals(expected, getContent(ofOs))
  }


  @Test
  def jarTest(): Unit = {
    val ofJar = FileSystem(getClass, "jar:/jp/hotbrain/db/migration/filesystemtest1")
    assertEquals(expected, getContent(ofJar))
  }

  private[this] def getContent(fileSystem: FileSystem): String = {
    getContentSub(new StringBuilder, "", fileSystem).toString
  }

  private[this] def getContentSub(sb: StringBuilder, tab: String, fileSystem: FileSystem): StringBuilder = {
    if (fileSystem.isFile) {
      sb.append(s"${tab}file: ${fileSystem.fileName}\n")
      sb.append(fileSystem.content.getOrElse("no content"))
    } else {
      sb.append(s"${tab}directory: ${fileSystem.fileName}\n")

      val newTab = tab + "  "
      fileSystem.children.foldLeft(sb) { (sb, child) => getContentSub(sb, newTab, child) }

      sb.append(s"$tab/directory: ${fileSystem.fileName}\n")
    }
  }
}
