package jp.hotbrain.db

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

package object migration {

  final val DateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  final val ZoneId_UTC: ZoneId = ZoneId.of("UTC")

  def nowString: String = DateTimeFormat.format(LocalDateTime.now(ZoneId_UTC))


  private[this] var _verbose: Boolean = false

  def setVerbose(verbose: Boolean): Unit = {
    _verbose = verbose
  }

  def verbose: Boolean = {
    _verbose
  }
}
