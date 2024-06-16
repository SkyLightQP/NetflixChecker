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
    val cost: Int = 0
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

@ConfigurationProperties(prefix = "supabase")
data class SupabaseConfiguration(
    val url: String = "",
    val key: String = ""
)

@ConfigurationProperties(prefix = "cors")
data class CorsOriginConfiguration(
    val origin: String = ""
)