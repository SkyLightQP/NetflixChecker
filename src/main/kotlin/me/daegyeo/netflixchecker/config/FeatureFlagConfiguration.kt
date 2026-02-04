package me.daegyeo.netflixchecker.config

import me.daegyeo.netflixchecker.entity.Setting
import me.daegyeo.netflixchecker.enum.FeatureFlagKey
import me.daegyeo.netflixchecker.table.Settings
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service

@Service
class FeatureFlagConfiguration {

    fun getString(key: FeatureFlagKey): String {
        return transaction {
            Setting.Companion.find { Settings.key eq key.key }
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
            val existing = Setting.Companion.find { Settings.key eq key.key }.firstOrNull()
            if (existing != null) {
                existing.value = value
            } else {
                Setting.Companion.new(key.key) {
                    this.key = key.key
                    this.value = value
                }
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
            Setting.Companion.all().associate { it.key to it.value }
        }
    }

    fun resetToDefault(key: FeatureFlagKey) {
        setString(key, key.defaultValue)
    }
}