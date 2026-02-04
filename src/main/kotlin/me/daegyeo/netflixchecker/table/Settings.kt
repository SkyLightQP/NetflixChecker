package me.daegyeo.netflixchecker.table

import org.jetbrains.exposed.dao.id.IdTable

object Settings : IdTable<String>("settings") {
    val key = varchar("key", length = 256)
    val value = varchar("value", length = 1024)

    override val id = key.entityId()
}