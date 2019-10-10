package jp.hotbrain.db.migration

import java.io.File

object Main extends App {
  if (args.length != 1) {
    println("Erro: 引数はベースフォルダ名一つ")
    System.exit(1)
  }
  try {
    MigratorConfig(FileSystem(FileSystem.prefixOs + new File(args(0)).getCanonicalPath))
      .foreach { conf => conf.iterator.foreach(_.exec(MigrationSchema.process(_, _, conf.sqls))) }
    System.exit(0)
  } catch {
    case ex: Throwable =>
      println(ex.getMessage)
      ex.printStackTrace()
      System.exit(2)
  }
}
