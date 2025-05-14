package fi.decentri.db.token

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.math.BigDecimal

/**
 * Table definition for token transfer events
 */
object TransferEvents : Table("token_transfer_events") {
    val id = integer("id").autoIncrement()
    val network = varchar("network", 50)
    val tokenAddress = varchar("token_address", 50)
    val contractAddress = varchar("contract_address", 50).nullable() // Optional monitored contract address
    val txHash = varchar("tx_hash", 66)
    val logIndex = integer("log_index")
    val blockNumber = long("block_number")
    val blockTimestamp = timestamp("block_timestamp")
    val fromAddress = varchar("from_address", 50)
    val toAddress = varchar("to_address", 50)
    val amount = decimal("amount", precision = 78, scale = 0) // To handle large ERC20 values with any precision
    val tokenSymbol = varchar("token_symbol", 20).nullable()
    val tokenDecimals = integer("token_decimals").nullable()

    override val primaryKey = PrimaryKey(id)
}