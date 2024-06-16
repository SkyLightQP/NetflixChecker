package me.daegyeo.netflixchecker.config

import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

class VaultConfiguration {
    companion object {
        private val configCache = mutableMapOf<String, String>()

        val DEPOSIT_TARGET_NAMES: String
            get() = getConfigInVault("deposit_target_names")

        val BANK_SITE_ID: String
            get() = getConfigInVault("bank_site_id")

        val BANK_SITE_PASSWORD: String
            get() = getConfigInVault("bank_site_password")

        val BANK_ACCOUNT_PASSWORD: String
            get() = getConfigInVault("bank_account_password")

        private fun getConfigInVault(name: String): String {
            return configCache.getOrPut(name) {
                transaction {
                    val conn = TransactionManager.current().connection
                    val sql = "SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = ?;"
                    val statement = conn.prepareStatement(sql, false)
                    statement.fillParameters(listOf(VarCharColumnType() to name))
                    val rs = statement.executeQuery()

                    if (rs.next()) rs.getString(1)
                    else throw RuntimeException("Not found config in vault.")
                }
            }
        }
    }
}