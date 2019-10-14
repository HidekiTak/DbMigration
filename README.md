# DbMigration
## Migration Library for MySQL
- no rollback
- programmable targeting schema name and connection setting
### added table for each database
- migration_jobs
- migration_semaphore
## Environment Variables for test
```
ENV=[prefix like "local"]
DB_MIG_CON_STR='jdbc:mysql://localhost/mysql?useSSL=false&user=[user name]&password=[password]'
```
## DDL directory / jar package
### Config File (*.conf)
####  by connection string
```
schema: "[schemaName]"
connection: "jdbc:mysql://localhost/mysql?useSSL=false&user=${DB_USER}&password=${DB_PASS}"
```
- value strings needs '"'
- if target schema name is equal to parent folder name, no need to set "schema".
- in connection string, by "${ENVIRONMENT_KEY}", you can refer environment variables.
ex. DB_USER, DB_PASS
####  by SingleSchema
```
single: "/net/example/db/SingleRuleFactoryEx"
```
```$scala
package net.example.db

import jp.hotbrain.db.migration.{SingleRule, SingleRuleFactory}

object SingleRuleFactoryEx extends SingleRule with SingleRuleFactory {

    def singleRule:SingleRule = this

    def getConnection(schemaName:String): (Connection, String) = {
        (
            schemaToConnection(schemaName),
            environmentedSchemaName(schemaName)
        )
    }
}
```
- if your system 
####  by Parameterized Multi Schema
```
multi: "/net/example/db/MultiRuleFactoryEx"
```
```$scala
package net.example.db

import jp.hotbrain.db.migration.{MultiRule, MultiRuleFactory}

object MultiRuleFactoryEx extends MultiRule with MultiRuleFactory {

    def multiRule: MultiRule = this

    def schemas(schemaBaseName: String): Seq[String] = Seq()

    def getConnection(schemaName:String): (Connection, String) = {
        (
            schemaToConnection(schemaName),
            environmentedSchemaName(schemaName)
        )
    }
}
```
### DDL File (*.sql)
- don't use "/* ------ */" comment.
- use "DROP (TABLE|PROCEDURE|FUNCTION|...) IF EXISTS ZZZZZZZZZ" before CREATE
## config file(*.conf)
### syntax
#### connection
##### single
##### multi
#### schema name
- only connection type is connection_string
schema: "[]"


### folder name = schema name
```
connection-string: "jdbc:mysql;//localhost:3306/mysql?useSSL=false&user=[user]&password=[password]"
```
## customize
