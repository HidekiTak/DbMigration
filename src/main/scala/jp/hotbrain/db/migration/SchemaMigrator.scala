package jp.hotbrain.db.migration

import java.io.{Reader, StringReader}
import java.sql.{Connection, DriverManager}

import scala.util.parsing.combinator.RegexParsers

private[migration] trait Callback {
  def exec(callback: (Connection, String) => Unit): Unit
}

private[migration] trait MigratorConfig {

  def folderName: String

  def schemaName: String

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

  def withSchemaName(schemaName: String): MigratorConfig

  def withFolderName(folderName: String): MigratorConfig

  private[this] var _sqls: Seq[(String, Seq[String])] = _

  def sqls: Seq[(String, Seq[String])] = _sqls

  def withSqls(sqls: Seq[(String, Seq[String])]): MigratorConfig = {
    _sqls = sqls
    this
  }

  def iterator: Iterator[Callback] = filter(_ => true)

  def filter(f: String => Boolean): Iterator[Callback] = new Iterator[Callback] {
    private[this] val schemas = list.filter(f).iterator

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


private[migration] object MigratorConfig {

  def apply(
             fileSystem: FileSystem,
             migrationDic: MigrationDic,
             formatter: (MigrationDic, String) => String): Seq[MigratorConfig] = {
    val children = fileSystem.children
    val ruleConf = children.sortBy(_.fileName).find(_.fileName.endsWith(".conf"))
    if (ruleConf.nonEmpty) {
      apply(fileSystem, ruleConf.head, migrationDic, children.filter(_.isFile), formatter).toList
    } else {
      children.filter(!_.isFile).sortBy(_.fileName).flatMap(apply(_, migrationDic, formatter))
    }
  }

  private[this] def apply(
                           parent: FileSystem,
                           configFile: FileSystem,
                           migrationDic: MigrationDic,
                           others: Seq[FileSystem],
                           formatter: (MigrationDic, String) => String): Option[MigratorConfig] = {
    MigratorConfigParser.parse(
      parent.fileName, configFile.content.getOrElse(""), migrationDic).map(_.withFolderName(parent.fileName).withSqls(toSqls(others, migrationDic, formatter)))
  }

  private[this] def toSqls(
                            sqls: Seq[FileSystem],
                            migrationDic: MigrationDic,
                            formatter: (MigrationDic, String) => String): Seq[(String, Seq[String])] = {
    sqls.filter(fs => fs.isFile && fs.fileName.endsWith(".sql") && fs.content.nonEmpty).map { fs =>
      (formatter(migrationDic, fs.fileName.substring(0, fs.fileName.length - 4)),
        MySqlSplitter.split(fs.fileName, fs.content.get, migrationDic))
    }
  }
}

private[migration] case class MigratorConfigString(
                                                    connectionString: String,
                                                    folderName: String,
                                                    schemaName: String
                                                  ) extends MigratorConfig {

  override protected[migration] final val list: Seq[String] = Seq(folderName)

  override protected def factory: String => Connection = (connectionString: String) => {
    DriverManager.getConnection(connectionString, "user", "password")
  }

  override def check(): Boolean = {
    MigratorConfigString.load(connectionString)
    true
  }

  override def withSchemaName(schemaName: String): MigratorConfig = {
    if (null == this.schemaName) {
      copy(schemaName = schemaName)
    } else {
      this
    }
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
    apply(connectionString).foreach(name => println(s"DbMigration: Driver loaded: $name"))
  }
}

private[migration] object MigratorConfigCon

private[migration] case class MigratorConfigCon(
                                                 singleRuleFactoryName: String,
                                                 folderName: String,
                                                 schemaName: String
                                               ) extends MigratorConfig {

  private lazy val singleRule: SingleRule = {
    val factory: SingleRuleFactory = ClassLoad.find(singleRuleFactoryName, Array(classOf[SingleRuleFactory]))
    if (null == factory) {
      throw new ClassNotFoundException(singleRuleFactoryName)
    }
    factory.singleRule
  }

  override protected[migration] final val list: Seq[String] = Seq(schemaName)

  override def factory: String => Connection = singleRule.connectionFor

  override def check(): Boolean = singleRule != null

  override def withSchemaName(schemaName: String): MigratorConfig = {
    if (null == this.schemaName) {
      copy(schemaName = schemaName)
    } else {
      this
    }
  }

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
                                                  folderName: String,
                                                  schemaName: String
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

  override def withSchemaName(schemaName: String): MigratorConfig = {
    if (null == this.schemaName) {
      copy(schemaName = schemaName)
    } else {
      this
    }
  }

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
      folderName = null,
      schemaName = null
    )
  }
}

private[migration] object MigratorConfigParser extends RegexParsers {

  private[this] def valueReaderEnv: Parser[ValueReader] = "${" ~> "[^}]+".r <~ "}" ^^ ValueReaderEnvironment.apply

  private[this] def valueReaderImmediate: Parser[ValueReader] = "[^\\$\"]+".r ^^ ValueReaderImmediate.apply

  private[this] def valueReader: Parser[ValueReader] = valueReaderEnv | valueReaderImmediate

  private[this] lazy val valueReaders: Parser[ValueReader] = rep(valueReader) ^^ ValueReaderMulti.apply


  private[this] def schemaName: Parser[ValueReader] = "(?i)(schema|catalog)(_?name)?".r ~ ":" ~ "\"" ~> valueReaders <~ "\""

  private[this] def constr: Parser[MigratorConfigBase] = "(?i)connection(_?string)?".r ~ ":" ~ "\"" ~> valueReaders <~ "\"" ^^ (vr => new MigratorConfigStringBase(vr))

  private[this] def connection: Parser[MigratorConfigBase] = "(?i)single".r ~ ":" ~ "\"" ~> valueReaders <~ "\"" ^^ (vr => new MigratorConfigConBase(vr))

  private[this] def each: Parser[MigratorConfigBase] = "(?i)multi".r ~ ":" ~ "\"" ~> valueReaders <~ "\"" ^^ (vr => new MigratorConfigEachBase(vr))

  private[this] trait MigratorConfigBase {
    def apply(migrationDic: MigrationDic): MigratorConfig
  }

  private[this] class MigratorConfigStringBase(vr: ValueReader) extends MigratorConfigBase {
    override def apply(migrationDic: MigrationDic): MigratorConfig = MigratorConfigString(vr.value(migrationDic), null, null)
  }

  private[this] class MigratorConfigConBase(vr: ValueReader) extends MigratorConfigBase {
    override def apply(migrationDic: MigrationDic): MigratorConfig = MigratorConfigCon(vr.value(migrationDic), null, null)
  }

  private[this] class MigratorConfigEachBase(vr: ValueReader) extends MigratorConfigBase {
    override def apply(migrationDic: MigrationDic): MigratorConfig = MigratorConfigEach(vr.value(migrationDic), null, null)
  }


  def parse(folderName: String, input: String, migrationDic: MigrationDic): Option[MigratorConfig] = {
    parseAll(folderName, new StringReader(input), migrationDic)
  }

  def parseAll(folderName: String, reader: Reader, migrationDic: MigrationDic): Option[MigratorConfig] =
    parseAll(rep(constr | connection | each | schemaName), reader) match {
      case Success(result: List[_], _) =>
        result.collectFirst {
          case x: MigratorConfigBase =>
            result.collectFirst {
              case vr: ValueReader => vr
            } match {
              case Some(vr) => x(migrationDic).withSchemaName(vr.value(migrationDic))
              case None => x(migrationDic)
            }
        }
      case Failure(msg, _) => println(s"""DbMigration: FAILURE("$folderName"): $msg""")
        throw new Exception(s"""DbMigration: FAILURE("$folderName"): $msg""")
      case Error(msg, next) => println(s"""DbMigration: ERROR("$folderName" line.${next.pos.line}-columns.${next.pos.column}): $msg""")
        throw new Exception(s"""DbMigration: ERROR("$folderName" line.${next.pos.line}-columns.${next.pos.column}): $msg""")
    }
}
