package jp.hotbrain.db.migration

import java.io.File
import java.util.regex.Pattern

import scala.io.Source

private[migration] trait FileSystem {

  def fileName: String

  def isFile: Boolean

  def children: Seq[FileSystem]

  def content: Option[String]

}

private[migration] object FileSystem {
  final val prefixJar: String = "jar:"
  final val prefixOs: String = "file:"

  def apply(name: String): FileSystem = {
    if (name.startsWith(prefixOs)) {
      FileSystemOs(new File(name.substring(5)))
    } else if (name.startsWith(prefixJar)) {
      FileSystemJar(getClass, name.substring(4))
    } else {
      throw new Exception(s"$name not found")
    }
  }
}

private[migration] object FileSystemOs

private[migration] case class FileSystemOs(
                                            file: File
                                          ) extends FileSystem {
  override def fileName: String = file.getName

  override def isFile: Boolean = file.isFile

  override def children: Seq[FileSystem] = if (file.isFile) {
    Nil
  } else {
    file.listFiles.sortBy(_.getName).map(FileSystemOs.apply)
  }

  override def content: Option[String] = if (!file.isFile) {
    None
  } else {
    val buf = Source.fromFile(file)
    try {
      Option(buf.mkString)
    } finally {
      buf.close()
    }
  }
}

private[migration] object FileSystemJar {

  def apply(clazz: Class[_], name: String): FileSystem = {

    val res = clazz.getResource(name)
    if (null == res) {
      println(s"DbMigration: resource not found: $name")
      return null
    } else {
      val content: String = {
        val stream = res.openStream()
        try {
          Source.fromInputStream(stream).mkString
        } finally {
          stream.close()
        }
      }
      if (content.isEmpty) {
        println(s"DbMigration: resource is empty: $name")
        return null
      } else {
        if (getFirstLine(content).flatMap(f => Option(clazz.getResource(name + "/" + f))).isEmpty) {
          FileSystemJarFile(name, Option(content))
        } else {
          FileSystemJarDir(clazz, name, content.split("\\r\\n|[\\n\\r\\u2028\\u2029\\u0085]"))
        }
      }
    }
  }

  private[this] final val regexFirstLine = Pattern.compile("([^\\n\\r\\u2028\\u2029\\u0085]*)")

  private[this] def getFirstLine(str: String): Option[String] = {
    val mat = regexFirstLine.matcher(str)
    if (mat.find()) {
      val firstLine = mat.group(1)
      if (firstLine.isEmpty) {
        None
      } else {
        Option(firstLine)
      }
    } else {
      None
    }
  }
}

private[migration] trait FileSystemJar extends FileSystem {

  def name: String

  override def fileName: String = name.substring(name.lastIndexOf('/') + 1)
}

private[migration] object FileSystemJarFile

private[migration] case class FileSystemJarFile(name: String, content: Option[String]) extends FileSystemJar {

  override def isFile: Boolean = true

  override def children: Seq[FileSystem] = Nil
}

private[migration] object FileSystemJarDir

private[migration] case class FileSystemJarDir(clazz: Class[_], name: String, subs: Seq[String]) extends FileSystemJar {

  override def isFile: Boolean = false

  override def content: Option[String] = None

  override def children: Seq[FileSystem] = if (isFile) {
    Nil
  } else {
    subs.map(b => FileSystemJar(clazz, name + "/" + b))
  }
}
