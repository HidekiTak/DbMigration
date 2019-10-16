package jp.hotbrain.db.migration

import java.util.Locale

import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.combinator.RegexParsers

private[migration] object MySqlSplitter extends RegexParsers {
  override final val whiteSpace = "".r


  def split(fileName: String, sql: String, migrationDic: MigrationDic): Seq[String] = {
    if (null == sql || sql.isEmpty) {
      return Nil
    }
    val current = new StringBuilder
    val lines = replace(fileName, sql, current, migrationDic)
      .split("(([\\r\\n]+[ \\t]*)+)")
      .map(_.trim)
      .filterNot(str => str.isEmpty || str.startsWith("--"))

    var delimiter = ";"
    val result = ArrayBuffer[String]()
    current.clear()
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
    result.toSeq // for scala 2.13
  }

  private[this] def valueReaderEnv: Parser[ValueReader] = "${" ~> "[A-Za-z][0-9A-Za-z\\-_]*".r <~ "}" ^^ ValueReaderEnvironment.apply

  private[this] def valueReaderImmediate: Parser[ValueReader] = """(?:(?:\\.)+|[^$]+|.[^{])+""".r ^^ ValueReaderImmediate.apply

  private[this] final val valueReaders: Parser[List[ValueReader]] = rep(valueReaderEnv | valueReaderImmediate)

  private[migration] def replace(fileName: String, line: String, sb: StringBuilder, migrationDic: MigrationDic): String = {
    parseAll(valueReaders, line) match {
      case Success(result: List[ValueReader], _) =>
        sb.clear()
        result.foldLeft(sb)((sb, vr) => vr.apply(sb, migrationDic)).toString
      case Failure(msg, _) =>
        throw new Exception(s"""FAILURE("$fileName"): $msg""")
      case Error(msg, next) =>
        throw new Exception(s"""ERROR("$fileName" line.${next.pos.line}-columns.${next.pos.column}): $msg""")
    }
  }
}
