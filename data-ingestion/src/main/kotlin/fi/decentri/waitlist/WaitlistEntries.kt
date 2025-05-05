package fi.decentri.waitlist

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Table definition for waitlist entries
 */
object WaitlistEntries : Table("waitlist_entries") {
    val id = long("id").autoIncrement()
    val email = text("email")
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}