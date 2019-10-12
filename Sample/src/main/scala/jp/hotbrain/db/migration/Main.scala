package jp.hotbrain.db.migration

import java.io.File

object Main extends App {
  val (opt, filenames) = args.partition(_.startsWith("-"))

  if (filenames.length != 1) {
    println("Error: [-d] baseFolderName")
    System.exit(1)
  }
  if (opt.length != 0 && opt.head != "-d") {
    println("Error: Option is only -d")
    System.exit(1)
  }
  println(s"dryRun: ${opt.length != 0}")
  try {
    MigratorConfig(FileSystem(FileSystem.prefixOs + new File(filenames(0)).getCanonicalPath))
      .foreach { conf => conf.iterator.foreach(_.exec(MigrationSchema.process(_, _, conf.sqls, dryRun = opt.length != 0))) }
    System.exit(0)
  } catch {
    case ex: Throwable =>
      println(ex.getMessage)
      ex.printStackTrace()
      System.exit(2)
  }
}
