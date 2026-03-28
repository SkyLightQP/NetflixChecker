package me.daegyeo.netflixchecker.config

import me.daegyeo.netflixchecker.entity.Setting
import me.daegyeo.netflixchecker.enum.FeatureFlagKey
import me.daegyeo.netflixchecker.table.Settings
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.upsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service

@Service
class FeatureFlagConfiguration {

    fun getString(key: FeatureFlagKey): String {
        return transaction {
            Setting.find { Settings.key eq key.key }
                .firstOrNull()
                ?.value
                ?: key.defaultValue
        }
    }

    fun getBoolean(key: FeatureFlagKey): Boolean {
        return getString(key).toBoolean()
    }

    fun getInt(key: FeatureFlagKey): Int {
        return getString(key).toIntOrNull() ?: key.defaultValue.toInt()
    }

    fun setString(key: FeatureFlagKey, value: String) {
        transaction {
            Settings.upsert(Settings.id) {
                it[Settings.id] = EntityID(key.key, Settings)
                it[Settings.value] = value
            }
        }
    }

    fun setBoolean(key: FeatureFlagKey, value: Boolean) {
        setString(key, value.toString())
    }

    fun setInt(key: FeatureFlagKey, value: Int) {
        setString(key, value.toString())
    }

    fun getAllFlags(): Map<String, String> {
        return transaction {
            val settingsByKey = Setting.all().associateBy { it.key }
            FeatureFlagKey.entries.associate { featureKey ->
                val dbValue = settingsByKey[featureKey.key]?.value
                featureKey.key to (dbValue ?: featureKey.defaultValue)
            }
        }
    }

    fun resetToDefault(key: FeatureFlagKey) {
        setString(key, key.defaultValue)
    }
}
