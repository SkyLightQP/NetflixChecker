package me.daegyeo.netflixchecker.table

import org.jetbrains.exposed.dao.id.IntIdTable

object DepositLogs : IntIdTable() {
    val who = varchar("who", length = 30)
    val cost = integer("cost")
    val time = integer("time")
}