package jp.hotbrain.db.migration

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
    Migrator.processDirectory(filenames(0), dryRun = opt.length != 0)
    System.exit(0)
  } catch {
    case ex: Throwable =>
      println(ex.getMessage)
      ex.printStackTrace()
      System.exit(2)
  }
}
