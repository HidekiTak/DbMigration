package jp.hotbrain.db.migration

import java.io.File
import java.sql.Connection

object Migrator {
  def processDirectory(
                        directoryName: String,
                        migrationDic: MigrationDic = MigrationDicDefault,
                        targetSchema: String = null,
                        targetFormatter: (MigrationDic, String) => String = noFormatter,
                        dryRun: Boolean = false): Unit = {
    processSub(FileSystemOs(new File(directoryName)), migrationDic, targetSchema, targetFormatter, dryRun)
  }

  private final val noFormatter: (MigrationDic, String) => String = (_, str) => str

  /**
   *
   * @param jarPackageName separate by "/"
   */
  def processPackage(clazz: Class[_],
                     jarPackageName: String,
                     migrationDic: MigrationDic = MigrationDicDefault,
                     targetSchema: String = null,
                     targetFormatter: (MigrationDic, String) => String = noFormatter,
                     dryRun: Boolean = false): Unit = {
    processSub(FileSystem(clazz, FileSystem.prefixJar + jarPackageName), migrationDic, targetSchema, targetFormatter, dryRun)
  }

  private[this] def processSub(
                                fileSystem: FileSystem,
                                migrationDic: MigrationDic,
                                targetSchema: String,
                                targetFormatter: (MigrationDic, String) => String,
                                dryRun: Boolean): Unit = {
    MigratorConfig(fileSystem, migrationDic, targetFormatter)
      .foreach(conf =>
        conf.filter(null == targetSchema || targetSchema == _).foreach {
          _.exec(MigrationSchema.process(conf.folderName, _, _, conf.sqls, dryRun))
        }
      )
  }

  def withConnection(
                      con: Connection,
                      clazz: Class[_],
                      jarPackageName: String,
                      migrationDic: MigrationDic = MigrationDicDefault,
                      targetSchema: String = null,
                      targetFormatter: (MigrationDic, String) => String = noFormatter,
                      dryRun: Boolean = false): Unit = {
    val currentCatalog = con.getCatalog
    try {
      MigratorConfig(FileSystem(clazz, FileSystem.prefixJar + jarPackageName), migrationDic, targetFormatter)
        .foreach(conf =>
          conf.filter(null == targetSchema || targetSchema == _).foreach {
            _.exec(con, MigrationSchema.process(conf.folderName, _, _, conf.sqls, dryRun))
          }
        )
    } catch {
      case ex: Throwable =>
        con.setCatalog(currentCatalog)
        throw ex
    }
  }

  def job(con: Connection, jobName: String, schema: String, callback: Connection => Unit): Unit = {
    MigrationSchema.job(jobName, con, schema, callback)
  }
}
