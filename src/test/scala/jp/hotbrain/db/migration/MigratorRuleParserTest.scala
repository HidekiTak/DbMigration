package jp.hotbrain.db.migration

import org.junit.Test
import org.junit.Assert._

class MigratorRuleParserTest {

  class MigrationDicImpl(
                          final val dic: Map[String, String]
                        ) extends MigrationDic {

    override def getMigrationParam(key: String): String = {
      dic.getOrElse(key, "")
    }
  }

  @Test
  def immediateTest(): Unit = {
    val result: MigratorConfig = MigratorConfigParser.parse(
      folderName = "folder",
      input =
        """
ConnectionString: "${AWS_REGION}_common"
""",
      migrationDic = new MigrationDicImpl(Map("AWS_REGION" -> "ap-northeast-1"))).orNull
    assertNotNull(result)
    assertTrue(result.isInstanceOf[MigratorConfigString])
    assertEquals("ap-northeast-1_common", result.asInstanceOf[MigratorConfigString].connectionString)
  }

  @Test
  def parseTest(): Unit = {
    println("parseTest")

    val result = MigratorConfigParser.parse(
      folderName = "parseTest",
      input = "single: \"jp.hotbrain.db.migration.RuleFactorySingleForTest\"",
      migrationDic = MigrationDicDefault).get

    assertEquals(
      "jp.hotbrain.db.migration.RuleFactorySingleForTest",
      result.asInstanceOf[MigratorConfigCon].singleRuleFactoryName)
    assertTrue(result.check())

    try {
      MigratorConfigParser.parse(
        folderName = "parseTest",
        input = "single: \"jp.hotbrain.db.migration.RuleFactorySingleForTest2\"",
        migrationDic = MigrationDicDefault).get.check()
      fail()
    } catch {
      case _: ClassNotFoundException =>
    }
  }
}
