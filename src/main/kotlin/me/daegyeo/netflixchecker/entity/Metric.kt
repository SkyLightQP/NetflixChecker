package me.daegyeo.netflixchecker.entity

import me.daegyeo.netflixchecker.table.Metrics
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Metric(key: EntityID<String>) : Entity<String>(key) {
    companion object : EntityClass<String, Metric>(Metrics)

    var key by Metrics.key
    var value by Metrics.value
}