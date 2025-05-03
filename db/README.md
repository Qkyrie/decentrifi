# Database Module

This module provides a centralized location for database-related functionality in the Decentrifi project.

## Features

- Common database connection factory
- Connection pooling with HikariCP
- Helpers for running database transactions in coroutine contexts
- Common interface for database configuration

## Usage

### Add as a dependency

```xml
<dependency>
    <groupId>fi.decentri</groupId>
    <artifactId>db</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Implement DatabaseConfig

```kotlin
import fi.decentri.db.config.DatabaseConfig

class MyDatabaseConfig : DatabaseConfig {
    override val jdbcUrl: String = "jdbc:postgresql://localhost:5432/mydatabase"
    override val username: String = "user"
    override val password: String = "password"
    override val maxPoolSize: Int = 10
}
```

### Initialize database

```kotlin
import fi.decentri.db.DatabaseFactory
import org.jetbrains.exposed.sql.Table

// Database tables
object MyTable : Table("my_table") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    
    override val primaryKey = PrimaryKey(id)
}

// Initialize database connection
val config = MyDatabaseConfig()
DatabaseFactory.init(config)

// Create tables if they don't exist
DatabaseFactory.initTables(MyTable)
```

### Use in coroutine contexts

```kotlin
import fi.decentri.db.DatabaseFactory

suspend fun fetchData(): List<MyData> = DatabaseFactory.dbQuery {
    MyTable.selectAll().map {
        MyData(it[MyTable.id], it[MyTable.name])
    }
}
```