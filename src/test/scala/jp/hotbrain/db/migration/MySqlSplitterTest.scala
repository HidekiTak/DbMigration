package jp.hotbrain.db.migration

import org.junit.Test
import org.junit.Assert._

class MySqlSplitterTest {

  @Test
  def splitTest2(): Unit = {
    val result =
      MySqlSplitter.split(
        "MySqlSplitterTest.splitTest2.sql",
        """
--- test
abc ---
de; ----
delimiter ;;
${ENV}
f;
f;;
""")

    val env = System.getenv("ENV")
    assertEquals(
      Seq(
        "abc\nde;",
        s"$env\nf;\nf;;"
      ), result)
  }
}
