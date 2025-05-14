package fi.decentri.infrastructure.repository.token

import fi.decentri.dataingest.model.TransferEvent
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.token.TransferEvents
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

/**
 * Repository for storing and retrieving token transfer events
 */
@ExperimentalTime
class TransferEventRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Insert a single transfer event
     */
    suspend fun insert(transferEvent: TransferEvent): Int {
        return dbQuery {
            TransferEvents.insert {
                it[network] = transferEvent.network
                it[tokenAddress] = transferEvent.tokenAddress
                it[contractAddress] = transferEvent.contractAddress
                it[txHash] = transferEvent.txHash
                it[logIndex] = transferEvent.logIndex
                it[blockNumber] = transferEvent.blockNumber
                it[blockTimestamp] = transferEvent.blockTimestamp
                it[fromAddress] = transferEvent.fromAddress
                it[toAddress] = transferEvent.toAddress
                it[amount] = transferEvent.amount
                it[tokenSymbol] = transferEvent.tokenSymbol
                it[tokenDecimals] = transferEvent.tokenDecimals
            }[TransferEvents.id]
        }
    }

    /**
     * Batch insert transfer events
     */
    suspend fun batchInsert(transferEvents: List<TransferEvent>): Boolean {
        return dbQuery {
            TransferEvents.batchInsert(transferEvents) { event ->
                this[TransferEvents.network] = event.network
                this[TransferEvents.tokenAddress] = event.tokenAddress
                this[TransferEvents.contractAddress] = event.contractAddress
                this[TransferEvents.txHash] = event.txHash
                this[TransferEvents.logIndex] = event.logIndex
                this[TransferEvents.blockNumber] = event.blockNumber
                this[TransferEvents.blockTimestamp] = event.blockTimestamp
                this[TransferEvents.fromAddress] = event.fromAddress
                this[TransferEvents.toAddress] = event.toAddress
                this[TransferEvents.amount] = event.amount
                this[TransferEvents.tokenSymbol] = event.tokenSymbol
                this[TransferEvents.tokenDecimals] = event.tokenDecimals
            }
            true
        }
    }

    /**
     * Find transfers for a specific contract (either as from or to address)
     */
    suspend fun findTransfersForContract(contractAddress: String, network: String): List<TransferEvent> {
        return dbQuery {
            TransferEvents.select {
                (TransferEvents.contractAddress eq contractAddress) and
                (TransferEvents.network eq network)
            }.map { toTransferEvent(it) }
        }
    }

    /**
     * Convert a database row to a TransferEvent model
     */
    private fun toTransferEvent(row: ResultRow): TransferEvent {
        return TransferEvent(
            id = row[TransferEvents.id],
            network = row[TransferEvents.network],
            tokenAddress = row[TransferEvents.tokenAddress],
            contractAddress = row[TransferEvents.contractAddress],
            txHash = row[TransferEvents.txHash],
            logIndex = row[TransferEvents.logIndex],
            blockNumber = row[TransferEvents.blockNumber],
            blockTimestamp = row[TransferEvents.blockTimestamp],
            fromAddress = row[TransferEvents.fromAddress],
            toAddress = row[TransferEvents.toAddress],
            amount = row[TransferEvents.amount],
            tokenSymbol = row[TransferEvents.tokenSymbol],
            tokenDecimals = row[TransferEvents.tokenDecimals]
        )
    }
}