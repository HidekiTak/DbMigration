package jp.hotbrain.db.migration

import org.junit.Test

class MySqlSplitterTest {

  @Test
  def splitTest(): Unit = {
    val result =
      """
--- test
abc ---
de

f
""".stripMargin.split("(([\\r\\n]+[ \\t]*)+|--[^\r\n]*)").map(_.trim).filterNot(str => str.isEmpty || str.startsWith("--"))

    println(result.mkString("\n"))
  }

  @Test
  def splitTest2(): Unit = {
    val result =
      MySqlSplitter.split(
        """
--- test
abc ---
de; ----
delimiter ;;

f;
f;;
""")

    println(result.mkString("\n"))
  }
}
