package me.daegyeo.netflixchecker.table

import org.jetbrains.exposed.dao.id.IdTable

object Metrics : IdTable<String>("metrics") {
    val key = varchar("key", length = 256)
    val value = varchar("value", length = 256)

    override val id = key.entityId()
}