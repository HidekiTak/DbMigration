package jp.hotbrain.db.migration

import java.io.{File, FileNotFoundException}
import java.net.URL
import java.util.jar.JarFile
import java.util.regex.Pattern

import scala.collection.mutable
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

  def apply(clazz: Class[_], name: String): FileSystem = {
    if (name.startsWith(prefixJar)) {
      val subName = name.substring(4)
      val res: URL = clazz.getResource(subName)
      if (null == res) {
        throw new FileNotFoundException(s"DbMigration: resource not found: $subName")
      }
      val protocol = res.getProtocol
      if (protocol == "jar") {
        FileSystemJar.apply(res, subName)
      } else {
        FileSystemOs(new File(res.getFile))
      }
    } else if (name.startsWith(prefixOs)) {
      FileSystemOs(new File(name.substring(5)).getAbsoluteFile)
      //    }
      //
      //
      //    if (name.startsWith(prefixOs)) {
      //      FileSystemOs(new File(name.substring(5)))
      //    } else if (name.startsWith(prefixJar)) {
      //      FileSystemJar(getClass, name.substring(4))
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

  def apply(res: URL, name: String): FileSystem = {
    val split = res.getPath.substring(5).split('!')
    val basePath = split(1)
    val basePathLen = basePath.length + 1
    val jarFile: JarFile = new JarFile(split.head)
    val entries = jarFile.entries()
    val top = FileSystemJarDir(basePath)
    while (entries.hasMoreElements) {
      val entry = entries.nextElement()
      val name = "/" + entry.getName
      if (name.startsWith(basePath)) {
        val tails = name.substring(basePathLen).split('/').filter(_.nonEmpty)
        if (tails.nonEmpty) {
          top.add(tails, name,
            if (entry.isDirectory) {
              FileSystemJarDir(name)
            } else {
              val is = jarFile.getInputStream(entry)
              try {
                FileSystemJarFile(name, Some(Source.fromInputStream(is).mkString))
              } finally {
                is.close()
              }
            })
        }
      }
    }
    top
  }

  //
  //  private[this] final val regexFirstLine = Pattern.compile("([^\\n\\r\\u2028\\u2029\\u0085]*)")
  //
  //  private[this] def getFirstLine(str: String): Option[String] = {
  //    val mat = regexFirstLine.matcher(str)
  //    if (mat.find()) {
  //      val firstLine = mat.group(1)
  //      if (firstLine.isEmpty) {
  //        None
  //      } else {
  //        Option(firstLine)
  //      }
  //    } else {
  //      None
  //    }
  //  }
}

private[migration] trait FileSystemJar extends FileSystem {

  def name: String

  override def fileName: String = name.substring(name.lastIndexOf('/') + 1)
}

private[migration] case class FileSystemJarFile(name: String, content: Option[String]) extends FileSystemJar {

  override def isFile: Boolean = true

  override def children: Seq[FileSystem] = Nil
}

/**
 *
 * @param name FullPath
 */
private[migration] case class FileSystemJarDir(name: String) extends FileSystemJar {

  override def isFile: Boolean = false

  override def content: Option[String] = None

  private[this] val _children: mutable.HashMap[String, FileSystemJar] = mutable.HashMap[String, FileSystemJar]()

  def add(path: Seq[String], fullPath: String, fsJar: FileSystemJar): FileSystemJarDir = {
    if (path.length == 1) {
      _children.update(path.head, fsJar)
    } else {
      _children.get(path.head).asInstanceOf[FileSystemJarDir].add(path.tail, fullPath, fsJar)
    }
    this
  }

  override def children: Seq[FileSystem] = _children.values.toSeq
}
