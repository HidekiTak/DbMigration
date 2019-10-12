package jp.hotbrain.db.migration

import java.io.File

object Migrator {
  def processDirectory(directoryName: String, dryRun: Boolean = false): Unit = {
    processSub(FileSystem(FileSystem.prefixOs + new File(directoryName).getCanonicalPath), dryRun)
  }

  def processPackage(jarPackageName: String, dryRun: Boolean = false): Unit = {
    processSub(FileSystem(FileSystem.prefixJar + jarPackageName), dryRun)
  }

  private[this] def processSub(fileSystem: FileSystem, dryRun: Boolean): Unit = {
    MigratorConfig(fileSystem)
      .foreach { conf => conf.iterator.foreach(_.exec(MigrationSchema.process(_, _, conf.sqls, dryRun))) }
  }
}
