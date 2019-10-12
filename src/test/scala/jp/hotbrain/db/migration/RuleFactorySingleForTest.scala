package jp.hotbrain.db.migration

import java.sql.{Connection, DriverManager}

abstract class RuleFactoryBaseForTest extends SingleRule {
  private[this] final val connectionString = System.getenv("DB_MIG_CON_STR")

  override def connectionFor(schemaName: String): Connection = {
    DriverManager.getConnection(connectionString)
  }
}

object RuleFactorySingleForTest extends RuleFactoryBaseForTest with SingleRuleFactory with SingleRule {

  override def singleRule: SingleRule = this
}

abstract class RuleFactoryMultiBaseForTest extends RuleFactoryBaseForTest with MultiRuleFactory with MultiRule {
  override def multiRule: MultiRule = this

  protected def ids: Seq[String]

  override def schemas(schemaName: String): Seq[String] = ids.map(schemaName + "_" + _ + "_db_migration")
}

class RuleFactoryMultiForTestInit() extends RuleFactoryMultiBaseForTest {

  override protected val ids: Seq[String] = Seq("00001", "00002")
}


class RuleFactoryMultiForTestAdded() extends RuleFactoryMultiBaseForTest with MultiRuleFactory with MultiRule {
  override protected val ids: Seq[String] = Seq("00001", "00002", "00003")
}
