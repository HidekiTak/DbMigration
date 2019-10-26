package jp.hotbrain.db.migration

import java.io.File
import java.sql.Connection

object Migrator {
  def processDirectory(
                        directoryName: String,
                        migrationDic: MigrationDic = MigrationDicDefault,
                        targetSchema: String = null,
                        dryRun: Boolean = false): Unit = {
    processSub(FileSystemOs(new File(directoryName)), migrationDic, targetSchema, dryRun)
  }

  /**
   *
   * @param jarPackageName separate by "/"
   */
  def processPackage(clazz: Class[_],
                     jarPackageName: String, migrationDic: MigrationDic = MigrationDicDefault, targetSchema: String = null, dryRun: Boolean = false): Unit = {
    processSub(FileSystem(clazz, FileSystem.prefixJar + jarPackageName), migrationDic, targetSchema, dryRun)
  }

  private[this] def processSub(fileSystem: FileSystem, migrationDic: MigrationDic, targetSchema: String, dryRun: Boolean): Unit = {
    MigratorConfig(fileSystem, migrationDic)
      .foreach(conf =>
        conf.filter(null == targetSchema || targetSchema == _).foreach {
          _.exec(MigrationSchema.process(conf.folderName, _, _, conf.sqls, dryRun))
        }
      )
  }

  def job(con: Connection, jobName: String, schema: String, callback: Connection => Unit): Unit = {
    MigrationSchema.job(jobName, con, schema, callback)
  }
}
