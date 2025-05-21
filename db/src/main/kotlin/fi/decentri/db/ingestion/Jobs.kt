package fi.decentri.db.ingestion

import fi.decentri.db.contract.Contracts
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb

/**
 * Enumeration of supported job types
 */
enum class JobType {
    EVENTS,
    RAW_INVOCATIONS,
    TOKEN_TRANSFERS
}

/**
 * Enumeration of job statuses
 */
enum class JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

/**
 * Table definition for ingestion jobs
 */
object Jobs : Table("ingestion_jobs") {
    val id = integer("id").autoIncrement()
    val type = enumerationByName("type", 20, JobType::class)
    val status = enumerationByName("status", 20, JobStatus::class)
    val contractId = integer("contract_id").references(Contracts.id)
    val metadata = jsonb<JsonElement>("metadata", Json) // Store job-specific metadata like start/end block
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val startedAt = timestamp("started_at").nullable()
    val completedAt = timestamp("completed_at").nullable()
    val errorMessage = text("error_message").nullable()

    override val primaryKey = PrimaryKey(id)
}