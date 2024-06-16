package me.daegyeo.netflixchecker.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date

object DepositLogs : IntIdTable("deposit_logs") {
    val who = varchar("who", length = 30)
    val cost = integer("cost")
    val date = date("date")
    val costMonth = integer("cost_month")
}