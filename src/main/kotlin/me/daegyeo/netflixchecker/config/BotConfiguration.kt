package me.daegyeo.netflixchecker.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "discord")
data class DiscordConfiguration(
    val botToken: String = "",
    val channel: String = "",
    val admin: String = ""
)

@ConfigurationProperties(prefix = "bank")
data class BankConfiguration(
    val cost: Int = 0,
    val siteId: String = "",
    val sitePassword: String = "",
    val accountPassword: String = ""
)

@ConfigurationProperties(prefix = "selenium")
data class SeleniumConfiguration(
    val useRemote: Boolean = false,
    val host: String = "",
    val useHeadless: Boolean = false
)

@ConfigurationProperties(prefix = "pop3")
data class Pop3Configuration(
    val host: String = "",
    val port: Int = 995,
    val user: String = "",
    val password: String = ""
)

@ConfigurationProperties(prefix = "sentry")
data class SentryConfiguration(
    val dsn: String = "",
)