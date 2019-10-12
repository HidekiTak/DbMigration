package jp.hotbrain.db.migration

// ---------------------------------------------------------------------------------------------------------------------
//
//  ValueReader
//

private[migration] trait ValueReader {
  def apply(sb: StringBuilder): StringBuilder

  def value: String
}

private[migration] case class ValueReaderImmediate(value: String) extends ValueReader {
  def apply(sb: StringBuilder): StringBuilder = sb.append(value)

}

private[migration] object ValueReaderImmediate

private[migration] case class ValueReaderEnvironment(key: String) extends ValueReader {

  override def value: String = System.getenv(key)

  def apply(sb: StringBuilder): StringBuilder = sb.append(value)

}

private[migration] object ValueReaderEnvironment

private[migration] case class ValueReaderMulti(vrs: Seq[ValueReader]) extends ValueReader {

  override def value: String = apply(new StringBuilder).toString()

  def apply(sb: StringBuilder): StringBuilder = vrs.foldLeft(sb)((sb, vr) => vr(sb))
}

private[migration] object ValueReaderMulti