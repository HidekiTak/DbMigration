package jp.hotbrain.db.migration

object MigrationDicDefault extends MigrationDic {
  override def getMigrationParam(key: String): String = {
    Option(System.getProperty(key)).orElse(Option(System.getenv(key))).getOrElse(throw new Exception(s"$key not found"))
  }
}
