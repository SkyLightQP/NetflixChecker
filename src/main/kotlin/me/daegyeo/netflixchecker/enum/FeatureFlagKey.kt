package me.daegyeo.netflixchecker.enum

enum class FeatureFlagKey(val key: String, val defaultValue: String) {
    ENABLE_BANK_CRAWLING_ERROR_ALERT("ENABLE_BANK_CRAWLING_ERROR_ALERT", "true"),
    ENABLE_CODE_CRAWLING_ERROR_ALERT("ENABLE_CODE_CRAWLING_ERROR_ALERT", "true"),
    MAX_RETRY_COUNT("MAX_RETRY_COUNT", "3"),
}
