package fi.decentri.db.event

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.postgresql.util.PGobject

/**
 * Table definition for raw_logs (blockchain events)
 */
object RawLogs : Table("raw_logs") {
    val id = integer("id").autoIncrement()
    val network = varchar("network", 50)
    val contractAddress = varchar("contract_address", 50)
    val txHash = varchar("tx_hash", 66)
    val logIndex = integer("log_index")
    val blockNumber = long("block_number")
    val blockTimestamp = timestamp("block_timestamp")
    val topic0 = varchar("topic_0", 66).nullable() // Event signature hash
    val topics = array<String>("topics") // All topics in event
    val data = text("data").nullable() // Raw event data
    val eventName = varchar("event_name", 100).nullable() // Decoded event name from ABI
    val decoded = customField("decoded", "jsonb") // Parsed event parameters

    override val primaryKey = PrimaryKey(id)
    
    /**
     * Helper method to create a JSONB field for Postgres
     */
    private fun customField(name: String, type: String) = 
        registerColumn<PGobject?>(name, PGObjectColumnType(type))
}

/**
 * Custom column type for PostgreSQL JSONB type
 */
class PGObjectColumnType(private val type: String) : org.jetbrains.exposed.sql.ColumnType() {
    override fun sqlType(): String = type
    
    override fun valueFromDB(value: Any): PGobject = when (value) {
        is PGobject -> value
        else -> {
            PGobject().apply {
                this.type = this@PGObjectColumnType.type
                this.value = value.toString()
            }
        }
    }
    
    override fun notNullValueToDB(value: Any): Any = when (value) {
        is PGobject -> value
        else -> {
            PGobject().apply {
                this.type = this@PGObjectColumnType.type
                this.value = value.toString()
            }
        }
    }
}