package jp.hotbrain.db.migration

object MigrationDicDefault extends MigrationDic {
  override def getMigrationParam(key: String): String = System.getenv(key)
}
