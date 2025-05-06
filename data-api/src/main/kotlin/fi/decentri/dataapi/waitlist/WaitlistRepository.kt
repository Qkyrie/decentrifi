package fi.decentri.dataapi.waitlist

import fi.decentri.db.DatabaseFactory.dbQuery
import fi.decentri.db.waitlist.WaitlistEntries
import org.jetbrains.exposed.sql.insert
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