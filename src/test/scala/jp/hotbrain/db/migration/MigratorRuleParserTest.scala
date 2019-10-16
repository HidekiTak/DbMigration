package jp.hotbrain.db.migration

import org.junit.Test
import org.junit.Assert._

class MigratorRuleParserTest {

  @Test
  def immediateTest(): Unit = {
    val result = MigratorConfigParser.parse(
      folderName = "folder",
      input =
        """
ConnectionString: "${ENV[AWS_REGION]}_common"
""",
      migrationDic = MigrationDicDefault)
    println(result)
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
