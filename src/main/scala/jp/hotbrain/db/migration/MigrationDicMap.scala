package jp.hotbrain.db.migration

case class MigrationDicMap(
                            params: Map[String, String]
                          ) extends MigrationDic {
  override def getMigrationParam(key: String): String = {
    params.get(key).orElse(Option(System.getProperty(key))).orElse(Option(System.getenv(key))).getOrElse(throw new Exception(s"$key not found"))
  }
}
