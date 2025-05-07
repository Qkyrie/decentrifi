package fi.decentri.db.ingestion
import fi.decentri.db.contract.Contracts
import org.jetbrains.exposed.sql.Table

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