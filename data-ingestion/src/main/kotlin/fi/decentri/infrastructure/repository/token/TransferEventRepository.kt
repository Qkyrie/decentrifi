package fi.decentri.infrastructure.repository.token

import fi.decentri.dataingest.model.TransferEvent
import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.token.TransferEvents
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

class TransferEventRepository {

    suspend fun batchInsert(events: List<TransferEvent>): Int =
        dbQuery {
            TransferEvents.batchInsert(
                events,        // ① data
                ignore = true, // ② skip dupes
                shouldReturnGeneratedValues = false
            ) { e ->
                this[TransferEvents.network] = e.network
                this[TransferEvents.txHash] = e.txHash
                this[TransferEvents.logIndex] = e.logIndex
                this[TransferEvents.tokenAddress] = e.tokenAddress
                this[TransferEvents.contractAddress] = e.contractAddress
                this[TransferEvents.blockNumber] = e.blockNumber
                this[TransferEvents.blockTimestamp] = e.blockTimestamp
                this[TransferEvents.fromAddress] = e.fromAddress
                this[TransferEvents.toAddress] = e.toAddress
                this[TransferEvents.amount] = e.amount
                this[TransferEvents.tokenSymbol] = e.tokenSymbol
                this[TransferEvents.tokenDecimals] = e.tokenDecimals
            }.size        // how many *new* rows actually landed
        }
}