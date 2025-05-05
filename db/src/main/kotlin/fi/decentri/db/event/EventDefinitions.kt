package fi.decentri.db.event

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Table definition for event_definitions to store ABI information for events
 */
object EventDefinitions : Table("event_definitions") {
    val id = integer("id").autoIncrement()
    val contractAddress = varchar("contract_address", 50)
    val eventName = varchar("event_name", 100)
    val signature = varchar("signature", 66) // topic_0 (event signature hash)
    val abiJson = text("abi_json") // JSON fragment of the event ABI
    val network = varchar("network", 50)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    // Create a unique constraint on contract address and event signature
    init {
        uniqueIndex(contractAddress, signature, network)
    }
}