package me.daegyeo.netflixchecker.enum

enum class FeatureFlagKey(val key: String, val defaultValue: String) {
    ENABLE_BANK_CRAWLING_ERROR_ALERT("ENABLE_BANK_CRAWLING_ERROR_ALERT", "true"),
    ENABLE_CODE_CRAWLING_ERROR_ALERT("ENABLE_CODE_CRAWLING_ERROR_ALERT", "true"),
    MAX_RETRY_COUNT("MAX_RETRY_COUNT", "3"), // TODO: Implement RETRY
    ONE_PERSON_RATE_COST("ONE_PERSON_RATE_COST", "1000"),
    DISCORD_CHANNEL_ID("DISCORD_CHANNEL_ID", "channel-id"),
    DISCORD_ADMIN_ID("DISCORD_ADMIN_ID", "admin-id"),
    BANK_COST("BANK_COST", "0"),
}
