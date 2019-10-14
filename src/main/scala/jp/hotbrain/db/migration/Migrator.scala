package jp.hotbrain.db.migration

import java.io.File

object Migrator {
  def processDirectory(directoryName: String, targetSchema: String = null, dryRun: Boolean = false): Unit = {
    processSub(FileSystem(FileSystem.prefixOs + new File(directoryName).getCanonicalPath), targetSchema, dryRun)
  }

  /**
   *
   * @param jarPackageName separate by "/"
   */
  def processPackage(jarPackageName: String, targetSchema: String = null, dryRun: Boolean = false): Unit = {
    processSub(FileSystem(FileSystem.prefixJar + jarPackageName), targetSchema, dryRun)
  }

  private[this] def processSub(fileSystem: FileSystem, targetSchema: String, dryRun: Boolean): Unit = {
    MigratorConfig(fileSystem)
      .foreach(conf =>
        conf.filter(null == targetSchema || targetSchema == _).foreach {
          _.exec(MigrationSchema.process(conf.folderName, _, _, conf.sqls, dryRun))
        }
      )
  }
}
