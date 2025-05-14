package fi.decentri.db.contract

import org.jetbrains.exposed.sql.Table


/**
 * Table definition for contract data including ABI and address
 */
object Contracts : Table("contracts") {
    val id = integer("id").autoIncrement()
    val address = varchar("address", 42)
    val abi = text("abi")
    val chain = varchar("chain", 64)
    val name = varchar("name", 128).nullable()
    val type = varchar("type", 32).nullable()

    override val primaryKey = PrimaryKey(id)
}