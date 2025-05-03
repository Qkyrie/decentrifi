package fi.decentri.dataingest.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Table definition for raw contract invocations
 */
object RawInvocations : Table("raw_invocations") {
    val id = integer("id").autoIncrement()
    val network = varchar("network", 64)
    val contractAddress = varchar("contract_address", 42)
    val blockNumber = long("block_number")
    val blockTimestamp = timestamp("block_timestamp")
    val txHash = varchar("tx_hash", 66)
    val fromAddress = varchar("from_address", 42)
    val functionSelector = varchar("function_selector", 10)
    //val inputArgs = jsonb("input_args", Map::class)
    val status = bool("status")
    val gasUsed = long("gas_used")
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Table definition for ingestion metadata tracking
 */
object IngestionMetadata : Table("ingestion_metadata") {
    val id = integer("id").autoIncrement()
    val key = varchar("key", 64)
    val value = varchar("value", 64)
    val contractId = integer("contract_id").references(Contracts.id).nullable()
    
    override val primaryKey = PrimaryKey(id)
}

/**
 * Table definition for contract data including ABI and address
 */
object Contracts : Table("contracts") {
    val id = integer("id").autoIncrement()
    val address = varchar("address", 42)
    val abi = text("abi")
    val chain = varchar("chain", 64)
    val name = varchar("name", 128).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}
