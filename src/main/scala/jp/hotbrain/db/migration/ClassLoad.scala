package jp.hotbrain.db.migration

import java.util.regex.Pattern

import scala.annotation.tailrec
import scala.collection.mutable
import scala.language.existentials
import scala.reflect.ClassTag

private[migration] object ClassLoad {

  class ReferencePath(final val path: String) extends AnyVal

  private[this] final val regex_multi_period: Pattern = Pattern.compile("""\.+""")

  def find[T](name: String, needInterface: Array[Class[_]] = null, refPath: Seq[ReferencePath] = null)(implicit tag: ClassTag[T]): T = {
    // まずobjectを検索してMODULE$でInstanceをGet
    // なければDefaultConstructorを使ってInstanceをGet
    if (null == refPath || refPath.isEmpty || name.contains('.')) {
      findObject(name, needInterface).orElse(findClass(name, needInterface)).getOrElse(throw new ClassNotFoundException(s"$name not found"))
    } else {
      refPath.foldLeft(None: Option[T])((r, path) =>
        r.orElse {
          val nm = regex_multi_period.matcher(s"${path.path}.$name").replaceAll(".")
          findObject(nm, needInterface).orElse(findClass(nm, needInterface))
        }
      ).getOrElse(throw new ClassNotFoundException(s"$name not found"))
    }
  }

  private[this] def findClass[T](name: String, needInterface: Array[Class[_]] = null)(implicit tag: ClassTag[T]): Option[T] = {
    try {
      val obj = Thread.currentThread().getContextClassLoader.loadClass(name)
      if (null == obj) {
        return None
      }
      val constructor = obj.getConstructor()
      if (null == constructor || !checkClass(obj, needInterface)) {
        return None
      }
      Option(constructor.newInstance().asInstanceOf[T])
    } catch {
      case _: ClassNotFoundException =>
        None
    }
  }

  private[this] def findObject[T](name: String, needInterface: Array[Class[_]] = null)(implicit tag: ClassTag[T]): Option[T] = {
    try {
      val obj = Thread.currentThread().getContextClassLoader.loadClass(name + "$")
      if (null == obj || !checkClass(obj, needInterface)) {
        return None
      }
      obj.getFields.find(_.getName == "MODULE$").map(_.get(null).asInstanceOf[T])
    } catch {
      case _: ClassNotFoundException =>
        None
    }
  }

  private[this] def checkClass[T](clazz: Class[_], needInterface: Array[Class[_]]): Boolean = {
    null == needInterface ||
      needInterface.length <= 0 || {
      val all = allClasses(clazz)
      needInterface.forall(all.contains)
    }
  }

  private[this] def allClasses(clazz: Class[_]): Seq[Class[_]] = {
    fillSuperClass(mutable.HashSet[Class[_]](clazz.getInterfaces: _*), clazz).toSeq
  }

  @tailrec
  private[this] def fillSuperClass(buf: mutable.HashSet[Class[_]], clazz: Class[_]): mutable.HashSet[Class[_]] = {
    val interfaces = clazz.getInterfaces
    if (null != interfaces && 0 < interfaces.length) {
      interfaces.foreach(buf.add)
    }
    val spr = clazz.getSuperclass
    if (null == spr) {
      buf
    } else {
      buf.add(spr)
      fillSuperClass(buf, spr)
    }
  }
}
