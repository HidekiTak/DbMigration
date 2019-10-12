# DbMigration
## MySQL Migration Library
- no rollback
- programmable schema name and connection setting 
### create table
- migration_jobs

- migration_semaphore

## Environment Variables for test
```
ENV=[prefix like "local"]
DB_MIG_CON_STR='jdbc:mysql://localhost/mysql?useSSL=false&user=[user name]&password=[password]'
```
## Directory
### schema name
- 00000.conf
confiture file
- .sql files ("00001_create_operators.sql" ...)


## config file(00000.conf)
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

