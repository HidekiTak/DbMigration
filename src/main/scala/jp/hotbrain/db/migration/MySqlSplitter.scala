package jp.hotbrain.db.migration

import java.util.Locale

import scala.collection.mutable.ArrayBuffer

private[migration] object MySqlSplitter {

  def split(sql: String): Seq[String] = {
    if (null == sql || sql.isEmpty) {
      return Nil
    }
    val lines = sql
      .split("(([\\r\\n]+[ \\t]*)+|--[^\r\n]*)")
      .map(_.trim)
      .filterNot(str => str.isEmpty || str.startsWith("--"))

    var delimiter = ";"
    val current = new StringBuilder
    val result = ArrayBuffer[String]()
    lines.foreach { line =>
      if (line.toLowerCase(Locale.ROOT).startsWith("delimiter ")) {
        if (current.nonEmpty) {
          result.append(current.toString)
          current.clear()
        }
        val splits = line.split("\\s")
        if (splits.length >= 2) {
          delimiter = splits(1)
        }
      } else {
        if (current.nonEmpty) {
          current.append('\n')
        }
        current.append(line)
        if (line.endsWith(delimiter)) {
          result.append(current.toString)
          current.clear()
        }
      }
    }
    if (current.nonEmpty) {
      result.append(current.toString)
    }
    result.toSeq
  }
}
