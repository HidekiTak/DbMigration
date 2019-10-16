package jp.hotbrain.db.migration

import java.io.File

object Migrator {
  def processDirectory(directoryName: String, migrationDic: MigrationDic = MigrationDicDefault, targetSchema: String = null, dryRun: Boolean = false): Unit = {
    processSub(FileSystem(FileSystem.prefixOs + new File(directoryName).getCanonicalPath), migrationDic, targetSchema, dryRun)
  }

  /**
   *
   * @param jarPackageName separate by "/"
   */
  def processPackage(jarPackageName: String, migrationDic: MigrationDic = MigrationDicDefault, targetSchema: String = null, dryRun: Boolean = false): Unit = {
    processSub(FileSystem(FileSystem.prefixJar + jarPackageName), migrationDic, targetSchema, dryRun)
  }

  private[this] def processSub(fileSystem: FileSystem, migrationDic: MigrationDic, targetSchema: String, dryRun: Boolean): Unit = {
    MigratorConfig(fileSystem, migrationDic)
      .foreach(conf =>
        conf.filter(null == targetSchema || targetSchema == _).foreach {
          _.exec(MigrationSchema.process(conf.folderName, _, _, conf.sqls, dryRun))
        }
      )
  }
}
