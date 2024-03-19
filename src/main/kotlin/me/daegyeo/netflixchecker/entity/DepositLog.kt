package me.daegyeo.netflixchecker.entity

import me.daegyeo.netflixchecker.table.DepositLogs
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class DepositLog(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DepositLog>(DepositLogs)

    var who by DepositLogs.who
    var cost by DepositLogs.cost
    var date by DepositLogs.date
}