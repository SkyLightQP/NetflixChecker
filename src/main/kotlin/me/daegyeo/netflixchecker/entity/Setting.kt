package me.daegyeo.netflixchecker.entity

import me.daegyeo.netflixchecker.table.Settings
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Setting(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Setting>(Settings)

    var key by Settings.key
    var value by Settings.value
}
