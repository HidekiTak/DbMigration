package jp.hotbrain.db.migration

// ---------------------------------------------------------------------------------------------------------------------
//
//  ValueReader
//

private[migration] trait ValueReader {

  def apply(sb: StringBuilder, migrationDic: MigrationDic): StringBuilder

  def value(migrationDic: MigrationDic): String
}

private[migration] case class ValueReaderImmediate(immediate: String) extends ValueReader {
  override def value(migrationDic: MigrationDic): String = immediate

  def apply(sb: StringBuilder, migrationDic: MigrationDic): StringBuilder = sb.append(value(migrationDic))

}

private[migration] object ValueReaderImmediate

private[migration] case class ValueReaderEnvironment(key: String) extends ValueReader {

  override def value(migrationDic: MigrationDic): String = migrationDic.getMigrationParam(key)

  def apply(sb: StringBuilder, migrationDic: MigrationDic): StringBuilder = sb.append(value(migrationDic))

}

private[migration] object ValueReaderEnvironment

private[migration] case class ValueReaderMulti(vrs: Seq[ValueReader]) extends ValueReader {

  override def value(migrationDic: MigrationDic): String = apply(new StringBuilder, migrationDic).toString()

  def apply(sb: StringBuilder, migrationDic: MigrationDic): StringBuilder = vrs.foldLeft(sb)((sb, vr) => vr(sb, migrationDic))
}

private[migration] object ValueReaderMulti