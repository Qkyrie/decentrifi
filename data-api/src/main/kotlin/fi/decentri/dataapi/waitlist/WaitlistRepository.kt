package fi.decentri.dataapi.waitlist

import fi.decentri.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Repository for managing waitlist entries
 */
class WaitlistRepository {
    suspend fun insert(email: String): Long {
        return dbQuery {
            WaitlistEntries.insert {
                it[WaitlistEntries.email] = email
                it[createdAt] = Instant.now()
            }[WaitlistEntries.id]
        }
    }
}