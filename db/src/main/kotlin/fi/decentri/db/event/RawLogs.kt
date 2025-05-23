package fi.decentri.db.event

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb

/**
 * Table definition for raw_logs (blockchain events)
 */
object RawLogs : Table("raw_logs") {
    val id = integer("id").autoIncrement()
    val network = varchar("network", 50)
    val contractAddress = varchar("contract_address", 42)
    val txHash = varchar("tx_hash", 66)
    val logIndex = integer("log_index")
    val blockNumber = long("block_number")
    val blockTimestamp = timestamp("block_timestamp")
    val topic0 = varchar("topic_0", 66).nullable() // Event signature hash
    val topics = jsonb<JsonElement>("topics", Json) // All topics in event
    val data = text("data").nullable() // Raw event data
    val eventName = varchar("event_name", 100).nullable() // Decoded event name from ABI
    val decoded = jsonb<JsonElement>("decoded", Json) // Parsed event parameters

    override val primaryKey = PrimaryKey(network, txHash, logIndex, name = "pk_raw_logs")
}