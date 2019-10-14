package jp.hotbrain.db.migration

object Main extends App {
  var dryRun: Boolean = false
  var targetIndex: Int = -1
  var target: String = _
  var directory: String = _
  for (i <- args.indices) {
    val arg = args(i)
    arg match {
      case "-d" => dryRun = true
      case "-t" => targetIndex = i + 1
      case _ if targetIndex == i => target = arg
      case _ => directory = arg
    }
  }

  if (directory == null) {
    println("Error: [-d(dry run)] [-t targetSchema] baseFolderName")
    System.exit(1)
  }
  if (dryRun) {
    println("dryRun")
  }
  if (null != target) {
    println(s"target: $target")
  }
  try {
    Migrator.processDirectory(directory, target, dryRun)
    System.exit(0)
  } catch {
    case ex: Throwable =>
      ex.printStackTrace()
      System.exit(2)
  }
}
