package fi.decentri.db.rawinvocation

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Table definition for raw_invocations
 */
object RawInvocations : Table("raw_invocations") {
    val id = integer("id").autoIncrement()
    val network = varchar("network", 50)
    val contractAddress = varchar("contract_address", 50)
    val blockNumber = long("block_number")
    val blockTimestamp = timestamp("block_timestamp")
    val txHash = varchar("tx_hash", 66)
    val fromAddress = varchar("from_address", 50)
    val functionSelector = varchar("function_selector", 10).nullable()
    val gasUsed = long("gas_used")

    override val primaryKey = PrimaryKey(id)
}