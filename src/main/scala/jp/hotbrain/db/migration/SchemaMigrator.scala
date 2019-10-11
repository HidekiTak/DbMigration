package jp.hotbrain.db.migration

import java.io.{Reader, StringReader}
import java.sql.{Connection, Driver, DriverManager}

import scala.util.parsing.combinator.RegexParsers

private[migration] trait Callback {
  def exec(callback: (Connection, String) => Unit): Unit
}

private[migration] trait MigratorConfig extends Iterable[Callback] {

  def check(): Boolean

  /**
   *
   * @return Scheama Name => Connection
   */
  protected def factory: String => Connection

  /**
   * SchemaNames
   */
  protected[migration] val list: Seq[String]

  def withFolderName(folderName: String): MigratorConfig

  private[this] var _sqls: Seq[(String, Seq[String])] = _

  def sqls: Seq[(String, Seq[String])] = _sqls

  def withSqls(sqls: Seq[(String, Seq[String])]): MigratorConfig = {
    _sqls = sqls
    this
  }

  override def iterator: Iterator[Callback] = new Iterator[Callback] {
    private[this] val schemas = list.iterator

    override def hasNext: Boolean = schemas.hasNext

    override def next(): Callback = {
      val schema = schemas.next()
      new Callback {
        override def exec(callback: (Connection, String) => Unit): Unit = {
          val con: Connection = factory(schema)
          try {
            callback(con, schema)
          } finally {
            con.close()
          }
        }
      }
    }
  }
}


object MigratorConfig {

  def apply(fileSystem: FileSystem): Seq[MigratorConfig] = {
    val (ruleConf, files) = fileSystem.children.partition(_.fileName == "00000.conf")
    if (ruleConf.nonEmpty) {
      println(s"process: ${fileSystem.fileName}")
      apply(fileSystem, ruleConf.head, files.filter(_.isFile)).toList
    } else {
      files.filter(!_.isFile).sortBy(_.fileName).flatMap(apply)
    }
  }

  private[this] def apply(parent: FileSystem, configFile: FileSystem, others: Seq[FileSystem]): Option[MigratorConfig] = {
    MigratorConfigParser.parse(parent.fileName, configFile.content.getOrElse("")).map(_.withFolderName(parent.fileName).withSqls(toSqls(others)))
  }

  private[this] def toSqls(sqls: Seq[FileSystem]): Seq[(String, Seq[String])] = {
    sqls.filter(fs => fs.isFile && fs.fileName.endsWith(".sql") && fs.content.nonEmpty).map { fs =>
      (fs.fileName.substring(0, fs.fileName.length - 4), MySqlSplitter.split(fs.content.get))
    }
  }
}

private[migration] case class MigratorConfigString(
                                                    connectionString: String,
                                                    folderName: String
                                                  ) extends MigratorConfig {

  def schemaName: String = folderName

  override protected[migration] final val list: Seq[String] = Seq(folderName)

  override protected def factory: String => Connection = (connectionString: String) => {
    DriverManager.getConnection(connectionString, "user", "password")
  }

  override def check(): Boolean = {
    MigratorConfigString.load(connectionString)
    true
  }

  override def withFolderName(folderName: String): MigratorConfig = {
    if (null == this.folderName) {
      copy(folderName = folderName)
    } else {
      this
    }
  }
}

private[migration] object MigratorConfigString {
  private[this] final val driverMap: Map[String, String] = Map(
    "jdbc.mysql://" -> "com.mysql.jdbc.Driver"
  )

  def apply(connectionString: String): Option[String] = {
    driverMap.find(tuple => connectionString.startsWith(tuple._1)).map(_._1)
  }

  def load(connectionString: String): Unit = {
    apply(connectionString).foreach(name => println(name))
  }
}

private[migration] object MigratorConfigCon

private[migration] case class MigratorConfigCon(
                                                 singleRuleFactoryName: String,
                                                 folderName: String
                                               ) extends MigratorConfig {
  def schemaName: String = folderName

  private lazy val singleRule: SingleRule = {
    val factory: SingleRuleFactory = ClassLoad.find(singleRuleFactoryName, Array(classOf[SingleRuleFactory]))
    if (null == factory) {
      throw new ClassNotFoundException(singleRuleFactoryName)
    }
    factory.singleRule
  }

  override protected[migration] final val list: Seq[String] = Seq(folderName)

  override def factory: String => Connection = singleRule.connectionFor

  override def check(): Boolean = singleRule != null

  override def withFolderName(folderName: String): MigratorConfig = {
    if (null == this.folderName) {
      copy(folderName = folderName)
    } else {
      this
    }
  }
}

private[migration] case class MigratorConfigEach(
                                                  eachRuleFactoryName: String,
                                                  folderName: String
                                                ) extends MigratorConfig {

  private lazy val eachRule: MultiRule = {
    val factory: MultiRuleFactory = ClassLoad.find(eachRuleFactoryName, Array(classOf[MultiRuleFactory]))
    if (null == factory) {
      throw new ClassNotFoundException(eachRuleFactoryName)
    }
    factory.multiRule
  }

  override protected[migration] lazy val list: Seq[String] = eachRule.schemas(folderName).toSeq

  override def factory: String => Connection = eachRule.connectionFor

  override def check(): Boolean = null != eachRule

  override def withFolderName(folderName: String): MigratorConfig = {
    if (null == this.folderName) {
      copy(folderName = folderName)
    } else {
      this
    }
  }
}


private[migration] object MigratorConfigEach {
  def apply(eachRuleFactory: String): MigratorConfigEach = {
    new MigratorConfigEach(
      ClassLoad.find(eachRuleFactory, Array(classOf[MultiRuleFactory])),
      null
    )
  }
}

private[migration] object MigratorConfigParser extends RegexParsers {

  private[this] def valueReaderEnv: Parser[ValueReader] = "${" ~> "[^}]+".r <~ "}" ^^ ValueReaderEnvironment.apply


  private[this] def valueReaderImmediate: Parser[ValueReader] = "[^\\$\"]+".r ^^ ValueReaderImmediate.apply

  private[this] def valueReader: Parser[ValueReader] = (valueReaderEnv | valueReaderImmediate)

  private[this] lazy val valueReaders: Parser[ValueReader] = rep(valueReader) ^^ ValueReaderMulti.apply


  def schemaName: Parser[ValueReader] = "(?i)schema_?name".r ~ ":" ~ "\"" ~> valueReaders <~ "\""

  def constr: Parser[MigratorConfig] = "(?i)connection_?string".r ~ ":" ~ "\"" ~> valueReaders <~ "\"" ^^ (vr => MigratorConfigString(vr.value, null))

  def connection: Parser[MigratorConfig] = "(?i)single".r ~ ":" ~ "\"" ~> valueReaders <~ "\"" ^^ (vr => MigratorConfigCon(vr.value, null))

  def each: Parser[MigratorConfig] = "(?i)multi".r ~ ":" ~ "\"" ~> valueReaders <~ "\"" ^^ (vr => MigratorConfigEach(vr.value, null))

  def parse(folderName: String, input: String): Option[MigratorConfig] = {
    parseAll(folderName, new StringReader(input))
  }

  def parseAll(folderName: String, reader: Reader): Option[MigratorConfig] =
    parseAll(rep(constr | connection | each | schemaName), reader) match {
      case Success(result: List[_], _) =>
        result.collectFirst {
          case x: MigratorConfig =>
            result.collectFirst {
              case vr: ValueReader => vr
            } match {
              case Some(vr) => x.withFolderName(vr.value)
              case None => x
            }
        }
      case Failure(msg, _) => println(s"""FAILURE("$folderName"): $msg""")
        throw new Exception(s"""FAILURE("$folderName"): $msg""")
      case Error(msg, next) => println(s"""ERROR("$folderName" line.${next.pos.line}-columns.${next.pos.column}): $msg""")
        throw new Exception(s"""ERROR("$folderName" line.${next.pos.line}-columns.${next.pos.column}): $msg""")
    }
}
